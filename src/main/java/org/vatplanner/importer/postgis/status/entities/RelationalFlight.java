package org.vatplanner.importer.postgis.status.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link Flight} extended for exchange with PostGIS.
 */
public class RelationalFlight extends Flight implements DirtyMark {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalFlight.class);

    private final DirtyEntityTracker tracker;

    private int databaseId = -1;

    public RelationalFlight(DirtyEntityTracker tracker, Member member, String callsign) {
        super(member, callsign);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public void markAsReconstructed(Report report) {
        markDirty();
        super.markAsReconstructed(report);
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public RelationalFlight setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
        return this;
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalFlight.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.isDirty(RelationalFlight.class, this);
    }

    @Override
    public void markClean() {
        tracker.recordAsClean(RelationalFlight.class, this);
    }

    public void insert(Connection db) throws SQLException {
        if (getDatabaseId() > 0) {
            throw new UnsupportedOperationException("updating flights is not implemented");
        }

        LOGGER.trace("INSERT flight: callsign {}", getCallsign());

        // TODO: save flag or number of reports if affected by reconstruction?
        PreparedStatement ps = db.prepareStatement("INSERT INTO flights (vatsimid, callsign) VALUES (?, ?) RETURNING flight_id");
        ps.setInt(1, getMember().getVatsimId());
        ps.setString(2, getCallsign());

        ResultSet rs = ps.executeQuery();

        rs.next();
        int flightId = rs.getInt("flight_id");

        rs.close();
        ps.close();

        if (flightId <= 0) {
            throw new RuntimeException("unexpected flight ID after insert: " + flightId);
        }

        setDatabaseId(flightId);
        markClean();
    }
}
