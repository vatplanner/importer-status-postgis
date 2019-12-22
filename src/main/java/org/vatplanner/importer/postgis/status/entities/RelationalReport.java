package org.vatplanner.importer.postgis.status.entities;

import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link Report} extended for exchange with PostGIS.
 */
public class RelationalReport extends Report implements DirtyMark {

    private final DirtyEntityTracker tracker;

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalReport(DirtyEntityTracker tracker, Instant recordTime) {
        super(recordTime);
        this.tracker = tracker;
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

    public RelationalReport setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
        return this;
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalReport.class, this);
    }

    @Override
    public void markClean() {
        if (databaseId < 1) {
            throw new UnsupportedOperationException("Entities must not be marked clean without a database ID!");
        }

        tracker.recordAsClean(RelationalReport.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.getDirtyEntities(RelationalReport.class).contains(this);
    }

}
