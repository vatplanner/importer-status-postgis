package org.vatplanner.importer.postgis.status.utils;

import java.time.Instant;

public class TimeHelpers {

    /**
     * Checks if given timestamp x is between start and end (inclusive).
     *
     * @param x timestamp to check
     * @param start earliest timestamp to test against (inclusive)
     * @param end latest timestamp to test against (inclusive)
     * @return true if x is between start and end (inclusive), false if out of
     * specified time period
     * @throws IllegalArgumentException if any parameter is null or end is
     * before start
     */
    public static boolean isBetween(Instant x, Instant start, Instant end) {
        if ((x == null) || (start == null) || (end == null)) {
            throw new IllegalArgumentException("parameters must not be null");
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must be after start");
        }

        return (start.compareTo(x) <= 0) && (x.compareTo(end) <= 0);
    }

}
