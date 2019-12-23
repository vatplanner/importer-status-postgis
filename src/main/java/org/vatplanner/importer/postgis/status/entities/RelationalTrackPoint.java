package org.vatplanner.importer.postgis.status.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalTrackPoint.class);

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

    public void insert(Connection db) throws SQLException {
        GeoCoordinates coords = getGeoCoordinates();
        RelationalReport report = (RelationalReport) getReport();
        RelationalFlight flight = (RelationalFlight) getFlight();

        if (flight == null) {
            throw new IllegalArgumentException("trackpoint is not associated to any flight");
        }

        int heading = getHeading();
        int groundSpeed = getGroundSpeed();
        int transponderCode = getTransponderCode();
        BarometricPressure qnh = getQnh();

        LOGGER.trace("INSERT trackpoint: report recorded {}, callsign {}, position {}, heading {}, GS {}, xpdr {}, QNH {}", report.getRecordTime(), flight.getCallsign(), coords, heading, groundSpeed, transponderCode, qnh);

        PreparedStatement ps = db.prepareStatement("INSERT INTO trackpoints (report_id, flight_id, geocoords, heading, groundspeed, transpondercode, qnhcinhg, qnhhpa) VALUES (?, ?, ST_MakePoint(?, ?, ?), ?, ?, ?, ?, ?)");
        ps.setInt(1, report.getDatabaseId());
        ps.setInt(2, flight.getDatabaseId());
        ps.setDouble(3, coords.getLongitude());
        ps.setDouble(4, coords.getLatitude());
        ps.setDouble(5, coords.getAltitudeFeet());

        if (heading >= 0) {
            ps.setInt(6, heading);
        } else {
            ps.setNull(6, Types.INTEGER);
        }

        if (groundSpeed >= 0) {
            ps.setInt(7, groundSpeed);
        } else {
            ps.setNull(7, Types.INTEGER);
        }

        if (transponderCode >= 0) {
            ps.setInt(8, transponderCode);
        } else {
            ps.setNull(8, Types.INTEGER);
        }

        // TODO: get rid of either centi-InHg or hPa if possible
        // TODO: instead save calculated flight level?
        if (qnh != null) {
            ps.setInt(9, (int) Math.round(qnh.getInchesOfMercury() * 100.0));
            ps.setInt(10, (int) Math.round(qnh.getHectopascals()));
        } else {
            ps.setNull(9, Types.INTEGER);
            ps.setNull(10, Types.INTEGER);
        }

        ps.execute();

        ps.close();

        markClean();
    }

}
