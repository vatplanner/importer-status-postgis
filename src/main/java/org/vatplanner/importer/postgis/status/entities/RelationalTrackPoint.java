package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.BarometricPressure;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.GeoCoordinates;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.TrackPoint;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link TrackPoint} extended for exchange with PostGIS.
 */
public class RelationalTrackPoint extends TrackPoint implements DirtyMark {

    private final DirtyEntityTracker tracker;

    public RelationalTrackPoint(DirtyEntityTracker tracker, Report report) {
        super(report);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public TrackPoint setFlight(Flight flight) {
        if (getFlight() != null) {
            throw new UnsupportedOperationException("trackpoints cannot change flights because database primary key would change");
        }

        markDirty();
        return super.setFlight(flight);
    }

    @Override
    public TrackPoint setGeoCoordinates(GeoCoordinates geoCoordinates) {
        markDirty();
        return super.setGeoCoordinates(geoCoordinates);
    }

    @Override
    public TrackPoint setGroundSpeed(int groundSpeed) {
        markDirty();
        return super.setGroundSpeed(groundSpeed);
    }

    @Override
    public TrackPoint setHeading(int heading) {
        markDirty();
        return super.setHeading(heading);
    }

    @Override
    public TrackPoint setQnh(BarometricPressure qnh) {
        markDirty();
        return super.setQnh(qnh);
    }

    @Override
    public TrackPoint setTransponderCode(int transponderCode) {
        markDirty();
        return super.setTransponderCode(transponderCode);
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalTrackPoint.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.isDirty(RelationalTrackPoint.class, this);
    }

    @Override
    public void markClean() {
        tracker.recordAsClean(RelationalTrackPoint.class, this);
    }

}
