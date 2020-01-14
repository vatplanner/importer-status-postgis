package org.vatplanner.importer.postgis.status;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.client.RawDataFileClient;
import org.vatplanner.importer.postgis.status.configuration.Configuration;
import org.vatplanner.importer.postgis.status.configuration.ImportConfiguration;
import org.vatplanner.importer.postgis.status.database.Database;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final Configuration config;
    private final RawDataFileClient archiveClient;
    private final Database database;
    private final MemoryWatchdog memoryWatchdog;

    private static final int EXIT_CODE_CONFIG_ERROR = 2;

    private Main(String[] args) throws IOException, TimeoutException {
        // load config
        String configPath = null;
        if (args.length > 0) {
            configPath = args[0];
        }
        config = new Configuration(configPath);

        // set up services
        archiveClient = new RawDataFileClient(config.getArchiveClientConfig());
        database = new Database(config.getDatabaseConfig());

        memoryWatchdog = new MemoryWatchdog(config.getMemoryConfig());
        memoryWatchdog.recordStartConsumption();
    }

    public static void main(String[] args) throws Exception {
        new Main(args).run();
    }

    private void run() {
        ImportConfiguration importConfig = config.getImportConfig();
        boolean allowImportOnEmptyDatabase = importConfig.isAllowImportOnEmptyDatabase();

        // protect database from configuration error: import should only be
        // allowed while database is empty
        if (allowImportOnEmptyDatabase && (database.getLatestFetchTime() != null)) {
            terminateIfEmptyDatabaseIsAllowed();
        }

        // run import until all data has been processed, memory consumption
        // grows too large or we hit some error
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

                terminateIfEmptyDatabaseIsAllowed();
            }

            LOGGER.info("maximum number of files ({}) has been imported, restarting clean to avoid OOM", importConfig.getMaxFilesBeforeRestart());

            // try to clear as much memory as possible
            importer = null;

            memoryWatchdog.cleanUpAndCheck();
        }
    }

    private void terminateIfEmptyDatabaseIsAllowed() {
        if (!config.getImportConfig().isAllowImportOnEmptyDatabase()) {
            return;
        }

        LOGGER.error("Database is not empty; disable configuration option to continue.");
        System.exit(EXIT_CODE_CONFIG_ERROR);
    }
}
