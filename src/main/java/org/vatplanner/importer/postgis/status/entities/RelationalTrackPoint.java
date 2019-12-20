package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.TrackPoint;

/**
 * {@link TrackPoint} extended for exchange with PostGIS.
 */
public class RelationalTrackPoint extends TrackPoint {
    // FIXME: implement

    public RelationalTrackPoint(Report report) {
        super(report);
    }

}
