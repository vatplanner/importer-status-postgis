package org.vatplanner.importer.postgis.status.entities;

import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Connection;
import org.vatplanner.dataformats.vatsimpublic.entities.status.ControllerRating;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;

/**
 * {@link Connection} extended for exchange with PostGIS.
 */
public class RelationalConnection extends Connection implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalConnection(Member member, Instant logonTime) {
        super(member, logonTime);
        markDirty();
    }

    @Override
    public Connection setHomeBase(String homeBase) {
        markDirty();
        return super.setHomeBase(homeBase);
    }

    @Override
    public Connection setProtocolVersion(int protocolVersion) {
        markDirty();
        return super.setProtocolVersion(protocolVersion);
    }

    @Override
    public Connection setRating(ControllerRating rating) {
        markDirty();
        return super.setRating(rating);
    }

    @Override
    public Connection setRealName(String realName) {
        markDirty();
        return super.setRealName(realName);
    }

    @Override
    public Connection setServerId(String serverId) {
        markDirty();
        return super.setServerId(serverId);
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
        isDirty = true;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

}
