package org.vatplanner.importer.postgis.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.importer.postgis.status.configuration.MemoryConfiguration;

/**
 * Simple cooperative watchdog killing the application in case garbage
 * collection fails to free enough memory to get usage back to configured
 * limits.
 *
 * <p>
 * {@link #recordStartConsumption()} needs to be called once before application
 * starts to process any data, recording the minimum expected memory GC would be
 * able to return to.
 * </p>
 *
 * <p>
 * {@link #cleanUpAndCheck()} triggers a GC run and checks if memory usage is
 * within limits afterwards. In case it is not, the application is killed with
 * the configured exit code.
 * </p>
 */
public class MemoryWatchdog {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryWatchdog.class);

    private final Runtime runtime;
    private final int maxPercentageIncreaseSinceFirstImport;
    private final int maxMemoryIncreaseSinceFirstImportMegaBytes;
    private final int exitCode;

    private long usedMemoryBeforeFirstImport;

    public MemoryWatchdog(MemoryConfiguration memoryConfig) {
        maxPercentageIncreaseSinceFirstImport = memoryConfig.getMaxPercentageIncreaseSinceFirstImport();
        maxMemoryIncreaseSinceFirstImportMegaBytes = memoryConfig.getMaxAbsoluteIncreaseSinceFirstImportMegaBytes();
        exitCode = memoryConfig.getExitCode();

        runtime = Runtime.getRuntime();
    }

    public void recordStartConsumption() {
        usedMemoryBeforeFirstImport = runtime.totalMemory();
    }

    public void cleanUpAndCheck() {
        long maxMemory = runtime.maxMemory();

        long usedMemoryBeforeGC = runtime.totalMemory();

        System.gc();

        long usedMemoryAfterGC = runtime.totalMemory();

        double percentUsedBeforeGC = (double) usedMemoryBeforeGC / maxMemory * 100.0;
        double percentUsedAfterGC = (double) usedMemoryAfterGC / maxMemory * 100.0;

        long memoryIncreaseSinceFirstImport = usedMemoryAfterGC - usedMemoryBeforeFirstImport;
        double percentIncreaseSinceFirstImport = (double) memoryIncreaseSinceFirstImport / usedMemoryBeforeFirstImport * 100.0;

        long memoryIncreaseSinceFirstImportMegaBytes = memoryIncreaseSinceFirstImport / 1024 / 1024;

        LOGGER.info(
                "before GC {}% used, after GC {}% used, increase of {} MB / {}% since application start",
                Math.round(percentUsedBeforeGC),
                Math.round(percentUsedAfterGC),
                memoryIncreaseSinceFirstImportMegaBytes,
                Math.round(percentIncreaseSinceFirstImport)
        );

        // quit before we risk hitting an OOM situation due to permanent increase in usage
        if (percentIncreaseSinceFirstImport > maxPercentageIncreaseSinceFirstImport) {
            LOGGER.error(
                    "permanent memory usage has increased by {} MB / {}% since application start which exceeds configured threshold of {}% - quitting to avoid OOM early, restart to continue import",
                    memoryIncreaseSinceFirstImportMegaBytes,
                    Math.round(percentIncreaseSinceFirstImport),
                    maxPercentageIncreaseSinceFirstImport
            );
            System.exit(exitCode);
        }

        if (memoryIncreaseSinceFirstImportMegaBytes > maxMemoryIncreaseSinceFirstImportMegaBytes) {
            LOGGER.error(
                    "permanent memory usage has increased by {} MB / {}% since application start which exceeds configured threshold of {} MB - quitting to avoid OOM early, restart to continue import",
                    memoryIncreaseSinceFirstImportMegaBytes,
                    Math.round(percentIncreaseSinceFirstImport),
                    maxMemoryIncreaseSinceFirstImportMegaBytes
            );
            System.exit(exitCode);
        }
    }
}
