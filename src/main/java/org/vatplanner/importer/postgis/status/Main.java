package org.vatplanner.importer.postgis.status;

import org.vatplanner.archiver.client.RawDataFileClient;

public class Main {

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

        StatusImport importer = new StatusImport(archiveClient, database);
        importer.importNextChunk();
    }
}
