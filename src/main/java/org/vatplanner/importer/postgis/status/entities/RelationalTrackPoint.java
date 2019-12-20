package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.BarometricPressure;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.GeoCoordinates;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.TrackPoint;

/**
 * {@link TrackPoint} extended for exchange with PostGIS.
 */
public class RelationalTrackPoint extends TrackPoint implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalTrackPoint(Report report) {
        super(report);
        markDirty();
    }

    @Override
    public TrackPoint setFlight(Flight flight) {
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
