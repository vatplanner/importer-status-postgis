package org.vatplanner.importer.postgis.status.entities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Connection;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link Connection} extended for exchange with PostGIS.
 */
public class RelationalConnection extends Connection implements DirtyMark {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalConnection.class);

    private final DirtyEntityTracker tracker;

    private int databaseId = -1;

    public RelationalConnection(DirtyEntityTracker tracker, Member member, Instant logonTime) {
        super(member, logonTime);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public Connection seenInReport(Report report) {
        markDirty();
        return super.seenInReport(report);
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalConnection.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.isDirty(RelationalConnection.class, this);
    }

    @Override
    public void markClean() {
        tracker.recordAsClean(RelationalConnection.class, this);
    }

    public void upsert(java.sql.Connection db) throws SQLException {
        if (getDatabaseId() > 0) {
            update(db);
        } else {
            insert(db);
        }
    }

    public void insert(java.sql.Connection db) throws SQLException {
        RelationalReport firstReport = (RelationalReport) getFirstReport();
        RelationalReport lastReport = (RelationalReport) getLastReport();

        LOGGER.trace(
            "INSERT connection: logon {}, first report {}, last report {}",
            getLogonTime(), firstReport.getRecordTime(), lastReport.getRecordTime() //
        );

        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO connections (vatsimid, logontime, firstreport_id, lastreport_id) VALUES (?, ?, ?, ?) RETURNING connection_id" //
        );
        ps.setInt(1, getMember().getVatsimId());
        ps.setTimestamp(2, Timestamp.from(getLogonTime()));
        ps.setInt(3, firstReport.getDatabaseId());
        ps.setInt(4, lastReport.getDatabaseId());

        ResultSet rs = ps.executeQuery();

        rs.next();
        int connectionId = rs.getInt("connection_id");

        rs.close();
        ps.close();

        if (connectionId <= 0) {
            throw new RuntimeException("unexpected connection ID after insert: " + connectionId);
        }

        setDatabaseId(connectionId);
        markClean();
    }

    public void update(java.sql.Connection db) throws SQLException {
        RelationalReport firstReport = (RelationalReport) getFirstReport();
        RelationalReport lastReport = (RelationalReport) getLastReport();

        LOGGER.trace(
            "UPDATE connection: ID {}, logon {}, first report {}, last report {}",
            getDatabaseId(), getLogonTime(), firstReport.getRecordTime(), lastReport.getRecordTime() //
        );

        PreparedStatement ps = db.prepareStatement(
            "UPDATE connections SET vatsimid=?, logontime=?, firstreport_id=?, lastreport_id=? WHERE connection_id=?" //
        );
        ps.setInt(1, getMember().getVatsimId());
        ps.setTimestamp(2, Timestamp.from(getLogonTime()));
        ps.setInt(3, firstReport.getDatabaseId());
        ps.setInt(4, lastReport.getDatabaseId());
        ps.setInt(5, getDatabaseId());

        int numAffected = ps.executeUpdate();
        if (numAffected != 1) {
            throw new RuntimeException("unexpected number of affected rows (" + numAffected + ") following update");
        }

        ps.close();

        markClean();
    }
}
