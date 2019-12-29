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
import org.vatplanner.dataformats.vatsimpublic.entities.status.StatusEntityFactory;
import org.vatplanner.dataformats.vatsimpublic.graph.GraphImport;
import org.vatplanner.dataformats.vatsimpublic.graph.GraphIndex;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntry;
import org.vatplanner.importer.postgis.status.entities.RelationalConnection;
import org.vatplanner.importer.postgis.status.entities.RelationalFacility;
import org.vatplanner.importer.postgis.status.entities.RelationalFlight;
import org.vatplanner.importer.postgis.status.entities.RelationalFlightPlan;
import org.vatplanner.importer.postgis.status.entities.RelationalReport;
import org.vatplanner.importer.postgis.status.entities.RelationalStatusEntityFactory;
import org.vatplanner.importer.postgis.status.entities.RelationalTrackPoint;

/**
 * Performs batch import of archived status information to PostGIS.
 */
public class StatusImport {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusImport.class);

    private final RawDataFileClient archiveClient;
    private final Database database;

    private final PackerMethod packerMethod = PackerMethod.ZIP_DEFLATE; // TODO: configure
    private final Duration fullGraphReloadTime = Duration.ofHours(3); // TODO: configure

    private final DirtyEntityTracker tracker = new DirtyEntityTracker();
    private final StatusEntityFactory statusEntityFactory = new RelationalStatusEntityFactory(tracker);
    private final GraphImport graphImport = new GraphImport(statusEntityFactory);

    private Instant latestImportedFetchTimestamp;

    public StatusImport(RawDataFileClient archiveClient, Database database) {
        this.archiveClient = archiveClient;
        this.database = database;
    }

    public int importNextChunk(int fileLimit) {
        if (latestImportedFetchTimestamp == null) {
            // FIXME: in case of no timestamp/empty database ask user to confirm by command line parameter to restart import for all available reports
            latestImportedFetchTimestamp = database.getLatestFetchTime();
        }

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

        // load partial graph from DB if not already loaded
        GraphIndex graphIndex = graphImport.getIndex();
        if (!graphIndex.hasReports()) {
            database.loadReportsSinceRecordTime(graphIndex, statusEntityFactory, latestImportedFetchTimestamp.minus(fullGraphReloadTime));
        }

        // check that tracker indicates no dirty entities
        int dirtyEntitiesBeforeImport = tracker.countDirtyEntities();
        if (dirtyEntitiesBeforeImport != 0) {
            LOGGER.error(
                    "{} dirty entities found before import: {} connections, {} facilities, {} flights, {} flight plans, {} reports, {} track points",
                    dirtyEntitiesBeforeImport,
                    tracker.getDirtyEntities(RelationalConnection.class).size(),
                    tracker.getDirtyEntities(RelationalFacility.class).size(),
                    tracker.getDirtyEntities(RelationalFlight.class).size(),
                    tracker.getDirtyEntities(RelationalFlightPlan.class).size(),
                    tracker.getDirtyEntities(RelationalReport.class).size(),
                    tracker.getDirtyEntities(RelationalTrackPoint.class).size()
            );
            throw new RuntimeException(dirtyEntitiesBeforeImport + " dirty entities after loading, expected clean state; aborting");
        }

        // get archive result
        List<ParsedDataFile> dataFiles;
        try {
            dataFiles = futureDataFiles.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Failed to load/parse data from archive for chunk from " + earliestFetchTimestamp + " to " + latestFetchTimestamp + " (file limit " + fileLimit + ").", ex);
        }

        // abort if we got no data files
        if (dataFiles.isEmpty()) {
            LOGGER.info("received no data files");
            return 0;
        }

        // import to graph
        LOGGER.debug("importing {} data files", dataFiles.size());

        Instant beforeImport = Instant.now();
        for (ParsedDataFile dataFile : dataFiles) {
            RelationalReport report = (RelationalReport) graphImport.importDataFile(dataFile.getContent());

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

        latestImportedFetchTimestamp = dataFiles.get(dataFiles.size() - 1).getFetchTime();

        // TODO: free up memory in JVM
        // TODO: periodic clean up in DB - data from connections and m:n tables is only needed while data is still recent (matching flights, fuzzy import)
        return dataFiles.size();
    }

}
