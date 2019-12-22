package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;

/**
 * {@link Flight} extended for exchange with PostGIS.
 */
public class RelationalFlight extends Flight implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalFlight(Member member, String callsign) {
        super(member, callsign);
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

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    @Override
    public void markDirty() {
        isDirty = true;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public void markClean() {
        isDirty = false;
    }

}
