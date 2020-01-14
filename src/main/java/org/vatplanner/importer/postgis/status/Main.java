package org.vatplanner.importer.postgis.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.client.RawDataFileClient;
import org.vatplanner.importer.postgis.status.configuration.Configuration;
import org.vatplanner.importer.postgis.status.configuration.ImportConfiguration;
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

        MemoryWatchdog memoryWatchdog = new MemoryWatchdog(config.getMemoryConfig());
        memoryWatchdog.recordStartConsumption();

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

            memoryWatchdog.cleanUpAndCheck();
        }
    }
}
