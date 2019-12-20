package org.vatplanner.importer.postgis.status.entities;

import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;

/**
 * {@link Report} extended for exchange with PostGIS.
 */
public class RelationalReport extends Report {
    // FIXME: implement

    public RelationalReport(Instant recordTime) {
        super(recordTime);
    }

}
