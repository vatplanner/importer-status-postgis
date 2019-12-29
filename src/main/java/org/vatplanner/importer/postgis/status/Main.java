package org.vatplanner.importer.postgis.status;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.client.RawDataFileClient;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        // load config
        String configPath = null;
        if (args.length > 0) {
            configPath = args[0];
        }
        Configuration config = new Configuration(configPath);

        // set up services
        RawDataFileClient archiveClient = new RawDataFileClient(config.getArchiveClientConfig());
        Database database = new Database(config.getDatabaseConfig());

        int maxFilesPerChunk = 200; // TODO: configure
        int maxFilesBeforeRestart = 1500; // TODO: configure
        boolean allowImportOnEmptyDatabase = false; // TODO: configure
        Instant emptyDatabaseEarliestFetchTime = Instant.MIN; // TODO: configure
        double maxPercentageIncreaseSinceFirstImport = 200.0; // TODO: configure
        double maxMemoryIncreaseSinceFirstImportMegaBytes = 500; // TODO: configure

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemoryBeforeFirstImport = runtime.totalMemory();

        while (true) {
            StatusImport importer = new StatusImport(archiveClient, database);
            importer.setAllowImportOnEmptyDatabase(allowImportOnEmptyDatabase);
            importer.setEarliestFetchTimestampEmptyDatabase(emptyDatabaseEarliestFetchTime);
            allowImportOnEmptyDatabase = false;

            int remainingFilesBeforeRestart = maxFilesBeforeRestart;
            while (remainingFilesBeforeRestart > 0) {
                int numImported = importer.importNextChunk(Integer.min(maxFilesPerChunk, remainingFilesBeforeRestart));
                if (numImported == 0) {
                    LOGGER.info("no further data, shutting down");
                    System.exit(0);
                }

                remainingFilesBeforeRestart -= numImported;

                System.gc();
            }

            LOGGER.info("maximum number of files ({}) has been imported, restarting clean to avoid OOM", maxFilesBeforeRestart);

            // try to clear as much memory as possible
            importer = null;

            long usedMemoryBeforeGC = runtime.totalMemory();

            System.gc();

            long usedMemoryAfterGC = runtime.totalMemory();

            double percentUsedBeforeGC = (double) usedMemoryBeforeGC / maxMemory * 100.0;
            double percentUsedAfterGC = (double) usedMemoryAfterGC / maxMemory * 100.0;

            long memoryIncreaseSinceFirstImport = usedMemoryAfterGC - usedMemoryBeforeFirstImport;
            double percentIncreaseSinceFirstImport = (double) memoryIncreaseSinceFirstImport / usedMemoryBeforeFirstImport * 100.0;

            long memoryIncreaseSinceFirstImportMegaBytes = memoryIncreaseSinceFirstImport / 1024 / 1024;

            LOGGER.info(
                    "before GC {}% used, after GC {}% used, increase of {} MB / {}% since application start",
                    Math.round(percentUsedBeforeGC),
                    Math.round(percentUsedAfterGC),
                    memoryIncreaseSinceFirstImportMegaBytes,
                    Math.round(percentIncreaseSinceFirstImport)
            );

            // memory "watchdog": quit before we risk hitting an OOM situation
            // due to permanent increase in usage
            if (percentIncreaseSinceFirstImport > maxPercentageIncreaseSinceFirstImport) {
                LOGGER.error(
                        "permanent memory usage has increased by {} MB / {}% since application start which exceeds configured threshold of {}% - quitting to avoid OOM early, restart to continue import",
                        memoryIncreaseSinceFirstImportMegaBytes,
                        Math.round(percentIncreaseSinceFirstImport),
                        Math.round(maxPercentageIncreaseSinceFirstImport)
                );
                System.exit(1);
            }

            if (memoryIncreaseSinceFirstImportMegaBytes > maxMemoryIncreaseSinceFirstImportMegaBytes) {
                LOGGER.error(
                        "permanent memory usage has increased by {} MB / {}% since application start which exceeds configured threshold of {} MB - quitting to avoid OOM early, restart to continue import",
                        memoryIncreaseSinceFirstImportMegaBytes,
                        Math.round(percentIncreaseSinceFirstImport),
                        maxMemoryIncreaseSinceFirstImportMegaBytes
                );
                System.exit(1);
            }
        }
    }
}
