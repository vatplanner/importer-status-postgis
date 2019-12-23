package org.vatplanner.importer.postgis.status;

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

        int maxFilesPerChunk = 200;
        int maxFilesBeforeRestart = 1500;

        StatusImport importer = new StatusImport(archiveClient, database);
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

        LOGGER.info("limit before restart reached, shutting down");
        System.exit(0);
    }
}
