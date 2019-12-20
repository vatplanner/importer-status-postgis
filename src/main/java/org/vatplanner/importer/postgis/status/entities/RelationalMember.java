package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;

/**
 * {@link Member} extended for exchange with PostGIS.
 */
public class RelationalMember extends Member implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalMember(int vatsimId) {
        super(vatsimId);
        markDirty();
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
