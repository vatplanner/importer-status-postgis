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
     *         specified time period
     * @throws IllegalArgumentException if any parameter is null or end is before
     *         start
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

    /**
     * Returns the minimum of both {@link Instant}s. Null input will be ignored and
     * leads to the other parameter being returned.
     *
     * @param a first Instant; may be null
     * @param b second Instant; may be null
     * @return minimum of both Instants; null values are ignored; null if both
     *         parameters are null
     */
    public static Instant min(Instant a, Instant b) {
        if (a == null) {
            return b;
        }

        if (b == null) {
            return a;
        }

        if (a.isBefore(b)) {
            return a;
        } else {
            return b;
        }
    }

}
