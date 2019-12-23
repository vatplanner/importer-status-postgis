package org.vatplanner.importer.postgis.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.client.RawDataFileClient;
import org.vatplanner.archiver.common.PackerMethod;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.graph.GraphImport;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntry;
import org.vatplanner.importer.postgis.status.entities.RelationalStatusEntityFactory;

/**
 * Performs batch import of archived status information to PostGIS.
 */
public class StatusImport {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusImport.class);

    private final RawDataFileClient archiveClient;
    private final Database database;

    private final PackerMethod packerMethod = PackerMethod.ZIP_DEFLATE; // TODO: configure
    private final int fileLimit = 5; // TODO: configure

    private final DirtyEntityTracker tracker = new DirtyEntityTracker();
    private final GraphImport graphImport = new GraphImport(new RelationalStatusEntityFactory(tracker));

    public StatusImport(RawDataFileClient archiveClient, Database database) {
        this.archiveClient = archiveClient;
        this.database = database;
    }

    public void importNextChunk() {
        Instant latestImportedFetchTimestamp = database.getLatestFetchTime();
        Instant earliestFetchTimestamp = (latestImportedFetchTimestamp != null) ? latestImportedFetchTimestamp.plusSeconds(1) : Instant.MIN;
        Instant latestFetchTimestamp = Instant.MAX;

        // request and parse data from archive
        CompletableFuture<List<ParsedDataFile>> futureDataFiles = archiveClient
                .request(packerMethod, earliestFetchTimestamp, latestFetchTimestamp, fileLimit)
                .thenApply(files -> {
                    return files
                            .stream()
                            .parallel()
                            .map(ParsedDataFile::new)
                            .sequential()
                            .sorted(Comparator.comparing(ParsedDataFile::getFetchTime))
                            .collect(Collectors.toList());
                });

        // TODO: read graph for last X hours from DB if not already loaded
        // get archive result
        List<ParsedDataFile> dataFiles;
        try {
            dataFiles = futureDataFiles.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Failed to load/parse data from archive for chunk from " + earliestFetchTimestamp + " to " + latestFetchTimestamp + " (file limit " + fileLimit + ").", ex);
        }

        // import to graph
        LOGGER.debug("importing {} data files", dataFiles.size());

        Instant beforeImport = Instant.now();
        for (ParsedDataFile dataFile : dataFiles) {
            Report report = graphImport.importDataFile(dataFile.getContent());

            if (report == null) {
                LOGGER.warn("Graph import rejected data file recorded {}, fetched {} by node {}, requested from {}, retrieved from {}", dataFile.getContent().getMetaData().getTimestamp(), dataFile.getFetchTime(), dataFile.getFetchNode(), dataFile.getFetchUrlRequested(), dataFile.getFetchUrlRetrieved());
                continue;
            }

            int parserRejectedLines = (int) dataFile
                    .getContent()
                    .getParserLogEntries()
                    .stream()
                    .filter(ParserLogEntry::isLineRejected)
                    .count();

            report
                    .setFetchNode(dataFile.getFetchNode())
                    .setFetchTime(dataFile.getFetchTime())
                    .setFetchUrlRequested(dataFile.getFetchUrlRequested())
                    .setFetchUrlRetrieved(dataFile.getFetchUrlRetrieved())
                    .setParserRejectedLines(parserRejectedLines)
                    .setParseTime(Instant.now());
        }
        Instant afterImport = Instant.now();

        LOGGER.debug("graph import took {} ms", Duration.between(beforeImport, afterImport).toMillis());

        // save dirty entities to database
        Instant beforeSave = Instant.now();
        database.saveDirtyEntities(tracker);
        Instant afterSave = Instant.now();
        LOGGER.debug("saving took {} ms", Duration.between(beforeSave, afterSave).toMillis());

        // TODO: free up memory
    }

}
