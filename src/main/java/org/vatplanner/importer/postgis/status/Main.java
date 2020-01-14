package org.vatplanner.importer.postgis.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.client.RawDataFileClient;
import org.vatplanner.importer.postgis.status.configuration.Configuration;
import org.vatplanner.importer.postgis.status.configuration.ImportConfiguration;
import org.vatplanner.importer.postgis.status.configuration.MemoryConfiguration;
import org.vatplanner.importer.postgis.status.database.Database;

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

        ImportConfiguration importConfig = config.getImportConfig();
        boolean allowImportOnEmptyDatabase = importConfig.isAllowImportOnEmptyDatabase();

        MemoryConfiguration memoryConfig = config.getMemoryConfig();
        int maxPercentageIncreaseSinceFirstImport = memoryConfig.getMaxPercentageIncreaseSinceFirstImport();
        int maxMemoryIncreaseSinceFirstImportMegaBytes = memoryConfig.getMaxAbsoluteIncreaseSinceFirstImportMegaBytes();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemoryBeforeFirstImport = runtime.totalMemory();

        while (true) {
            StatusImport importer = new StatusImport(archiveClient, database);
            importer.setAllowImportOnEmptyDatabase(allowImportOnEmptyDatabase);
            importer.setEarliestFetchTimestampEmptyDatabase(importConfig.getEmptyDatabaseEarliestFetchTime());
            allowImportOnEmptyDatabase = false;

            int remainingFilesBeforeRestart = importConfig.getMaxFilesBeforeRestart();
            while (remainingFilesBeforeRestart > 0) {
                int numImported = importer.importNextChunk(Integer.min(importConfig.getMaxFilesPerChunk(), remainingFilesBeforeRestart));
                if (numImported == 0) {
                    LOGGER.info("no further data, shutting down");
                    System.exit(0);
                }

                remainingFilesBeforeRestart -= numImported;

                System.gc();
            }

            LOGGER.info("maximum number of files ({}) has been imported, restarting clean to avoid OOM", importConfig.getMaxFilesBeforeRestart());

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
                        maxPercentageIncreaseSinceFirstImport
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
