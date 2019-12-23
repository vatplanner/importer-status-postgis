package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link Flight} extended for exchange with PostGIS.
 */
public class RelationalFlight extends Flight implements DirtyMark {

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

}
