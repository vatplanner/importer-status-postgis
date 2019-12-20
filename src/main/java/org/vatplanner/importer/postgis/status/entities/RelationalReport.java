package org.vatplanner.importer.postgis.status.entities;

import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;

/**
 * {@link Report} extended for exchange with PostGIS.
 */
public class RelationalReport extends Report implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalReport(Instant recordTime) {
        super(recordTime);
        markDirty();
    }

    @Override
    public Report setFetchNode(String fetchNode) {
        markDirty();
        return super.setFetchNode(fetchNode);
    }

    @Override
    public Report setFetchTime(Instant fetchTime) {
        markDirty();
        return super.setFetchTime(fetchTime);
    }

    @Override
    public Report setFetchUrlRequested(String fetchUrlRequested) {
        markDirty();
        return super.setFetchUrlRequested(fetchUrlRequested);
    }

    @Override
    public Report setFetchUrlRetrieved(String fetchUrlRetrieved) {
        markDirty();
        return super.setFetchUrlRetrieved(fetchUrlRetrieved);
    }

    @Override
    public Report setNumberOfConnectedClients(int numberOfConnectedClients) {
        markDirty();
        return super.setNumberOfConnectedClients(numberOfConnectedClients);
    }

    @Override
    public Report setParseTime(Instant parseTime) {
        markDirty();
        return super.setParseTime(parseTime);
    }

    @Override
    public Report setParserRejectedLines(int parserRejectedLines) {
        markDirty();
        return super.setParserRejectedLines(parserRejectedLines);
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
