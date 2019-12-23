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

    public RelationalReport(DirtyEntityTracker tracker, Instant recordTime) {
        super(recordTime);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public Report setFetchNode(String fetchNode) {
        markDirty();
        return super.setFetchNode(fetchNode);
    }

    @Override
    public Report setFetchTime(Instant fetchTime) {
        markDirty();
        return super.setFetchTime(fetchTime);
    }

    @Override
    public Report setFetchUrlRequested(String fetchUrlRequested) {
        markDirty();
        return super.setFetchUrlRequested(fetchUrlRequested);
    }

    @Override
    public Report setFetchUrlRetrieved(String fetchUrlRetrieved) {
        markDirty();
        return super.setFetchUrlRetrieved(fetchUrlRetrieved);
    }

    @Override
    public Report setNumberOfConnectedClients(int numberOfConnectedClients) {
        markDirty();
        return super.setNumberOfConnectedClients(numberOfConnectedClients);
    }

    @Override
    public Report setParseTime(Instant parseTime) {
        markDirty();
        return super.setParseTime(parseTime);
    }

    @Override
    public Report setParserRejectedLines(int parserRejectedLines) {
        markDirty();
        return super.setParserRejectedLines(parserRejectedLines);
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
