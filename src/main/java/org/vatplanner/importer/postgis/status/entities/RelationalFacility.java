package org.vatplanner.importer.postgis.status.entities;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Connection;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Facility;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FacilityType;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link Facility} extended for exchange with PostGIS.
 */
public class RelationalFacility extends Facility implements DirtyMark {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalFacility.class);

    private final DirtyEntityTracker tracker;

    private boolean hasRecordInDatabase = false;

    public RelationalFacility(DirtyEntityTracker tracker, String name) {
        super(name);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public Facility setConnection(Connection connection) {
        markDirty();
        return super.setConnection(connection);
    }

    @Override
    public Facility setFrequencyKilohertz(int frequencyKilohertz) {
        // frequency affects check for providing ATC service
        markDirty();
        return super.setFrequencyKilohertz(frequencyKilohertz);
    }

    @Override
    public Facility setType(FacilityType type) {
        markDirty();
        return super.setType(type);
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalFacility.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.isDirty(RelationalFacility.class, this);
    }

    @Override
    public void markClean() {
        tracker.recordAsClean(RelationalFacility.class, this);
    }

    public RelationalFacility setHasRecordInDatabase(boolean hasRecordInDatabase) {
        this.hasRecordInDatabase = hasRecordInDatabase;
        return this;
    }

    public void insert(java.sql.Connection db) throws SQLException {
        if (hasRecordInDatabase) {
            throw new UnsupportedOperationException("updating facilities is not implemented: name \"" + getName() + "\", connection ID " + ((RelationalConnection) getConnection()).getDatabaseId());
        }

        if (!providesATCService()) {
            LOGGER.trace("facility {} does not provide ATC service and thus is irrelevant for this application, rejecting database insertion", getName());
            markClean();
            return;
        }

        RelationalConnection connection = (RelationalConnection) getConnection();
        LOGGER.trace("INSERT facility: name {}, connection {}", getName(), connection.getDatabaseId());

        PreparedStatement ps = db.prepareStatement("INSERT INTO facilities (name, connection_id) VALUES (?, ?)");
        ps.setString(1, getName());
        ps.setInt(2, connection.getDatabaseId());

        ps.executeUpdate();

        ps.close();

        setHasRecordInDatabase(true);
        markClean();
    }
}
