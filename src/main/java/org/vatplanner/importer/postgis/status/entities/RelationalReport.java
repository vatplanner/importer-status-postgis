package org.vatplanner.importer.postgis.status.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.importer.postgis.status.Caches;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link Report} extended for exchange with PostGIS.
 */
public class RelationalReport extends Report implements DirtyMark {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalReport.class);

    private final DirtyEntityTracker tracker;

    private int databaseId = -1;

    private Instant fetchTime;
    private String fetchUrlRequested;
    private String fetchUrlRetrieved;
    private String fetchNode;

    private Instant parseTime;
    private int parserRejectedLines = -1;

    public RelationalReport(DirtyEntityTracker tracker, Instant recordTime) {
        super(recordTime);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public Report setNumberOfConnectedClients(int numberOfConnectedClients) {
        markDirty();
        return super.setNumberOfConnectedClients(numberOfConnectedClients);
    }

    /**
     * Returns the timestamp when the source data file was fetched (requested)
     * from VATSIM servers.
     *
     * <p>
     * This is an extra field to keep track of application-specific but
     * generally useful meta-information.
     * </p>
     *
     * @return timestamp of fetching the source data file
     */
    public Instant getFetchTime() {
        return fetchTime;
    }

    public RelationalReport setFetchTime(Instant fetchTime) {
        this.fetchTime = fetchTime;
        return this;
    }

    /**
     * Returns the URL originally requested to retrieve the source data file.
     * This should usually be a URL listed in the {@link NetworkInformation}
     * used at time of request.
     *
     * <p>
     * This is an extra field to keep track of application-specific but
     * generally useful meta-information.
     * </p>
     *
     * @return URL data file was originally requested from (before redirects)
     */
    public String getFetchUrlRequested() {
        return fetchUrlRequested;
    }

    public RelationalReport setFetchUrlRequested(String fetchUrlRequested) {
        this.fetchUrlRequested = fetchUrlRequested;
        return this;
    }

    /**
     * Returns the URL the source data file was actually retrieved from after
     * following all redirects. This may be a different URL than those listed in
     * {@link NetworkInformation}.
     *
     * <p>
     * This is an extra field to keep track of application-specific but
     * generally useful meta-information.
     * </p>
     *
     * @return URL data file was actually retrieved from (after redirects)
     */
    public String getFetchUrlRetrieved() {
        return fetchUrlRetrieved;
    }

    public RelationalReport setFetchUrlRetrieved(String fetchUrlRetrieved) {
        this.fetchUrlRetrieved = fetchUrlRetrieved;
        return this;
    }

    /**
     * Returns the identification of the cluster node who fetched the data file.
     *
     * <p>
     * This is an extra field to keep track of application-specific but
     * generally useful meta-information.
     * </p>
     *
     * @return ID of cluster node who fetched the data file
     */
    public String getFetchNode() {
        return fetchNode;
    }

    public RelationalReport setFetchNode(String fetchNode) {
        this.fetchNode = fetchNode;
        return this;
    }

    /**
     * Returns the timestamp when a data file was parsed/processed.
     *
     * <p>
     * This is an extra field to keep track of application-specific but
     * generally useful meta-information.
     * </p>
     *
     * @return timestamp of parsing/processing data file
     */
    public Instant getParseTime() {
        return parseTime;
    }

    public RelationalReport setParseTime(Instant parseTime) {
        this.parseTime = parseTime;
        return this;
    }

    /**
     * Returns the number of lines rejected by the parser. A number >0 indicated
     * loss of information. If not set, a negative value will be returned.
     *
     * @return number of lines rejected by parser; negative if not set
     */
    public int getParserRejectedLines() {
        return parserRejectedLines;
    }

    public RelationalReport setParserRejectedLines(int parserRejectedLines) {
        this.parserRejectedLines = parserRejectedLines;
        return this;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public RelationalReport setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
        return this;
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalReport.class, this);
    }

    @Override
    public void markClean() {
        if (databaseId < 1) {
            throw new UnsupportedOperationException("Entities must not be marked clean without a database ID!");
        }

        tracker.recordAsClean(RelationalReport.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.isDirty(RelationalReport.class, this);
    }

    public void insert(Connection db, Caches caches) throws SQLException {
        if (getDatabaseId() > 0) {
            throw new UnsupportedOperationException("updating reports is not implemented");
        }

        LOGGER.trace("INSERT report: record time {}, fetch time {}", getRecordTime(), getFetchTime());

        // TODO: replace deduplication by DB functions
        int fetchNodeId = caches.getFetchNodes().getId(getFetchNode());
        int fetchUrlRequestedId = caches.getFetchUrls().getId(getFetchUrlRequested());
        int fetchUrlRetrievedId = caches.getFetchUrls().getId(getFetchUrlRetrieved());

        // TODO: record number of skipped clients
        // TODO: record number of reconstructed flights?
        PreparedStatement ps = db.prepareStatement("INSERT INTO reports (recordtime, connectedclients, fetchtime, fetchnode_id, fetchurlrequested_id, fetchurlretrieved_id, parsetime, parserrejectedlines) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING report_id");
        ps.setTimestamp(1, Timestamp.from(getRecordTime()));
        ps.setInt(2, getNumberOfConnectedClients());
        ps.setTimestamp(3, Timestamp.from(getFetchTime()));
        if (fetchNodeId > 0) {
            ps.setInt(4, fetchNodeId);
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.setInt(5, fetchUrlRequestedId);
        if (fetchUrlRetrievedId > 0) {
            ps.setInt(6, fetchUrlRetrievedId);
        } else {
            ps.setNull(6, Types.INTEGER);
        }
        ps.setTimestamp(7, Timestamp.from(getParseTime()));
        ps.setInt(8, getParserRejectedLines());

        ResultSet rs = ps.executeQuery();

        rs.next();
        int reportId = rs.getInt("report_id");

        rs.close();
        ps.close();

        if (reportId <= 0) {
            throw new RuntimeException("unexpected report ID after insert: " + reportId);
        }

        setDatabaseId(reportId);
        markClean();
    }
}
