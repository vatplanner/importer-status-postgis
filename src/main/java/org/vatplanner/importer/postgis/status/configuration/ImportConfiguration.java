package org.vatplanner.importer.postgis.status.configuration;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration controlling the overall import process.
 */
public class ImportConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportConfiguration.class);

    private int maxFilesPerChunk;
    private int maxFilesBeforeRestart;
    private boolean allowImportOnEmptyDatabase;
    private Instant emptyDatabaseEarliestFetchTime;

    public int getMaxFilesPerChunk() {
        return maxFilesPerChunk;
    }

    public ImportConfiguration setMaxFilesPerChunk(int maxFilesPerChunk) {
        LOGGER.debug("setting maxFilesPerChunk to {}", maxFilesPerChunk);
        this.maxFilesPerChunk = maxFilesPerChunk;
        return this;
    }

    public int getMaxFilesBeforeRestart() {
        return maxFilesBeforeRestart;
    }

    public ImportConfiguration setMaxFilesBeforeRestart(int maxFilesBeforeRestart) {
        LOGGER.debug("setting maxFilesBeforeRestart to {}", maxFilesBeforeRestart);
        this.maxFilesBeforeRestart = maxFilesBeforeRestart;
        return this;
    }

    public boolean isAllowImportOnEmptyDatabase() {
        return allowImportOnEmptyDatabase;
    }

    public ImportConfiguration setAllowImportOnEmptyDatabase(boolean allowImportOnEmptyDatabase) {
        LOGGER.debug("setting allowImportOnEmptyDatabase to {}", allowImportOnEmptyDatabase);
        this.allowImportOnEmptyDatabase = allowImportOnEmptyDatabase;
        return this;
    }

    public Instant getEmptyDatabaseEarliestFetchTime() {
        return emptyDatabaseEarliestFetchTime;
    }

    public ImportConfiguration setEmptyDatabaseEarliestFetchTime(Instant emptyDatabaseEarliestFetchTime) {
        LOGGER.debug("setting emptyDatabaseEarliestFetchTime to {}", emptyDatabaseEarliestFetchTime);
        this.emptyDatabaseEarliestFetchTime = emptyDatabaseEarliestFetchTime;
        return this;
    }

}
