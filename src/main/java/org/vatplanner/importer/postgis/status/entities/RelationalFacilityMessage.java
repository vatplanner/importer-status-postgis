package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Facility;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FacilityMessage;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;

/**
 * {@link FacilityMessage} extended for exchange with PostGIS.
 */
public class RelationalFacilityMessage extends FacilityMessage implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalFacilityMessage(Facility facility) {
        super(facility);
        markDirty();
    }

    @Override
    public FacilityMessage seenInReport(Report report) {
        markDirty();
        return super.seenInReport(report);
    }

    @Override
    public FacilityMessage setMessage(String message) {
        markDirty();
        return super.setMessage(message);
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

}
