package org.vatplanner.importer.postgis.status.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration for the memory watchdog.
 */
public class MemoryConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryConfiguration.class);

    private int maxPercentageIncreaseSinceFirstImport;
    private int maxAbsoluteIncreaseSinceFirstImportMegaBytes;
    private int exitCode;

    public int getMaxAbsoluteIncreaseSinceFirstImportMegaBytes() {
        return maxAbsoluteIncreaseSinceFirstImportMegaBytes;
    }

    public MemoryConfiguration setMaxAbsoluteIncreaseSinceFirstImportMegaBytes(int maxAbsoluteIncreaseSinceFirstImportMegaBytes) {
        LOGGER.debug(
            "setting maxAbsoluteIncreaseSinceFirstImportMegaBytes to {}",
            maxAbsoluteIncreaseSinceFirstImportMegaBytes //
        );
        this.maxAbsoluteIncreaseSinceFirstImportMegaBytes = maxAbsoluteIncreaseSinceFirstImportMegaBytes;
        return this;
    }

    public int getMaxPercentageIncreaseSinceFirstImport() {
        return maxPercentageIncreaseSinceFirstImport;
    }

    public MemoryConfiguration setMaxPercentageIncreaseSinceFirstImport(int maxPercentageIncreaseSinceFirstImport) {
        LOGGER.debug("setting maxPercentageIncreaseSinceFirstImport to {}", maxPercentageIncreaseSinceFirstImport);
        this.maxPercentageIncreaseSinceFirstImport = maxPercentageIncreaseSinceFirstImport;
        return this;
    }

    public int getExitCode() {
        return exitCode;
    }

    public MemoryConfiguration setExitCode(int exitCode) {
        LOGGER.debug("setting exitCode to {}", exitCode);
        this.exitCode = exitCode;
        return this;
    }

}
