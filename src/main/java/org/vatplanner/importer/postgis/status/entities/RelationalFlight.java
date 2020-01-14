package org.vatplanner.importer.postgis.status.entities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Connection;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightEvent;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.TrackPoint;
import org.vatplanner.importer.postgis.status.database.Caches;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;
import org.vatplanner.importer.postgis.status.database.StrictEnumCache;

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
    public Flight addConnection(Connection connection) {
        markDirty();
        return super.addConnection(connection);
    }

    /*
    @Override
    public void markAsReconstructed(Report report) {
        markDirty();
        super.markAsReconstructed(report);
    }
     */
    @Override
    public void markEvent(TrackPoint trackPoint, FlightEvent event) {
        markDirty();
        super.markEvent(trackPoint, event);
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

    public void insert(java.sql.Connection db, Caches caches) throws SQLException {
        if (getDatabaseId() <= 0) {
            // insert currently only stores immutable information, updates mean that
            // connections (m:n table) or events might have changed
            insertFlight(db);
        }

        insertConnections(db);
        insertEvents(db, caches.getFlightEvents());

        markClean();
    }

    private void insertFlight(java.sql.Connection db) throws SQLException {
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
    }

    private void insertConnections(java.sql.Connection db) throws SQLException {
        for (Connection connection : getConnections()) {
            insertConnection(db, (RelationalConnection) connection);
        }
    }

    private void insertConnection(java.sql.Connection db, RelationalConnection connection) throws SQLException {
        LOGGER.trace("INSERT m:n flight={} connection={}", getDatabaseId(), connection.getDatabaseId());

        PreparedStatement ps = db.prepareStatement("INSERT INTO connections_flights (flight_id, connection_id) VALUES (?, ?) ON CONFLICT DO NOTHING");
        ps.setInt(1, getDatabaseId());
        ps.setInt(2, connection.getDatabaseId());

        ps.executeUpdate();

        ps.close();
    }

    private void insertEvents(java.sql.Connection db, StrictEnumCache<FlightEvent> flightEvents) throws SQLException {
        for (Map.Entry<TrackPoint, FlightEvent> entry : this.getEvents().entrySet()) {
            insertEvent(db, flightEvents, entry.getKey(), entry.getValue());
        }
    }

    private void insertEvent(java.sql.Connection db, StrictEnumCache<FlightEvent> flightEvents, TrackPoint trackPoint, FlightEvent event) throws SQLException {
        int reportId = ((RelationalReport) trackPoint.getReport()).getDatabaseId();
        int eventId = flightEvents.getId(event);

        LOGGER.trace("INSERT m:n flight={} report={} flightevent={}/{}", getDatabaseId(), reportId, event, eventId);

        PreparedStatement ps = db.prepareStatement("INSERT INTO trackpoints_flightevents (flight_id, report_id, flightevent_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING");
        ps.setInt(1, getDatabaseId());
        ps.setInt(2, reportId);
        ps.setInt(3, eventId);

        ps.executeUpdate();

        ps.close();
    }
}
