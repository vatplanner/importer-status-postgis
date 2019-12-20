package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Facility;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FacilityType;

/**
 * {@link Facility} extended for exchange with PostGIS.
 */
public class RelationalFacility extends Facility implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalFacility(String name) {
        super(name);
        markDirty();
    }

    @Override
    public Facility setFrequencyKilohertz(int frequencyKilohertz) {
        markDirty();
        return super.setFrequencyKilohertz(frequencyKilohertz);
    }

    @Override
    public Facility setType(FacilityType type) {
        markDirty();
        return super.setType(type);
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
