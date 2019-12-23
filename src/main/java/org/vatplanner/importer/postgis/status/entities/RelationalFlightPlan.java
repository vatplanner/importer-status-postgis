package org.vatplanner.importer.postgis.status.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.CommunicationMode;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlan;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlanType;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.SimpleEquipmentSpecification;
import org.vatplanner.dataformats.vatsimpublic.entities.status.WakeTurbulenceCategory;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

/**
 * {@link FlightPlan} extended for exchange with PostGIS.
 */
public class RelationalFlightPlan extends FlightPlan implements DirtyMark {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalFlight.class);

    private final DirtyEntityTracker tracker;

    public RelationalFlightPlan(DirtyEntityTracker tracker, Flight flight, int revision) {
        super(flight, revision);
        this.tracker = tracker;
        markDirty();
    }

    @Override
    public FlightPlan seenInReport(Report report) {
        // TODO: check if and how saved, might be irrelevant
        markDirty();
        return super.seenInReport(report);
    }

    @Override
    public FlightPlan setAircraftType(String aircraftType) {
        markDirty();
        return super.setAircraftType(aircraftType);
    }

    @Override
    public FlightPlan setAlternateAirportCode(String alternateAirportCode) {
        markDirty();
        return super.setAlternateAirportCode(alternateAirportCode);
    }

    @Override
    public FlightPlan setAltitudeFeet(int altitudeFeet) {
        markDirty();
        return super.setAltitudeFeet(altitudeFeet);
    }

    @Override
    public FlightPlan setCommunicationMode(CommunicationMode communicationMode) {
        markDirty();
        return super.setCommunicationMode(communicationMode);
    }

    @Override
    public FlightPlan setDepartureAirportCode(String departureAirportCode) {
        markDirty();
        return super.setDepartureAirportCode(departureAirportCode);
    }

    @Override
    public FlightPlan setDepartureTimeActual(Instant departureTimeActual) {
        markDirty();
        return super.setDepartureTimeActual(departureTimeActual);
    }

    @Override
    public FlightPlan setDepartureTimePlanned(Instant departureTimePlanned) {
        markDirty();
        return super.setDepartureTimePlanned(departureTimePlanned);
    }

    @Override
    public FlightPlan setDestinationAirportCode(String destinationAirportCode) {
        markDirty();
        return super.setDestinationAirportCode(destinationAirportCode);
    }

    @Override
    public FlightPlan setEstimatedTimeEnroute(Duration estimatedTimeEnroute) {
        markDirty();
        return super.setEstimatedTimeEnroute(estimatedTimeEnroute);
    }

    @Override
    public FlightPlan setEstimatedTimeFuel(Duration estimatedTimeFuel) {
        markDirty();
        return super.setEstimatedTimeFuel(estimatedTimeFuel);
    }

    @Override
    public FlightPlan setFlightPlanType(FlightPlanType flightPlanType) {
        markDirty();
        return super.setFlightPlanType(flightPlanType);
    }

    @Override
    public FlightPlan setRemarks(String remarks) {
        // TODO: check if we save remarks, otherwise ignore this field
        markDirty();
        return super.setRemarks(remarks);
    }

    @Override
    public FlightPlan setRoute(String route) {
        markDirty();
        return super.setRoute(route);
    }

    @Override
    public FlightPlan setSimpleEquipmentSpecification(SimpleEquipmentSpecification simpleEquipmentSpecification) {
        markDirty();
        return super.setSimpleEquipmentSpecification(simpleEquipmentSpecification);
    }

    @Override
    public FlightPlan setTrueAirSpeed(int trueAirSpeed) {
        markDirty();
        return super.setTrueAirSpeed(trueAirSpeed);
    }

    @Override
    public FlightPlan setWakeTurbulenceCategory(WakeTurbulenceCategory wakeTurbulenceCategory) {
        markDirty();
        return super.setWakeTurbulenceCategory(wakeTurbulenceCategory);
    }

    @Override
    public void markDirty() {
        tracker.recordAsDirty(RelationalFlightPlan.class, this);
    }

    @Override
    public boolean isDirty() {
        return tracker.isDirty(RelationalFlightPlan.class, this);
    }

    @Override
    public void markClean() {
        tracker.recordAsClean(RelationalFlightPlan.class, this);
    }

    public void insert(Connection db) throws SQLException {
        // TODO: change to UPSERT

        RelationalFlight flight = (RelationalFlight) getFlight();

        LOGGER.trace("INSERT flightplan: flight {}, revision {}, first report {}, callsign {}, altitude {}, dep {}, dest {}, alt {}, type {}", flight.getDatabaseId(), getRevision(), getReportFirstSeen().getRecordTime(), flight.getCallsign(), getAltitudeFeet(), getDepartureAirportCode(), getDestinationAirportCode(), getAlternateAirportCode(), getAircraftType());

        PreparedStatement ps = db.prepareStatement("INSERT INTO flightplans (flight_id, revision, firstseen_report_id, flightplantype, departuretimeplanned, route, altitudefeet, minutesenroute, minutesfuel, departureairport, destinationairport, alternateairport, aircrafttype) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setInt(1, flight.getDatabaseId());
        ps.setInt(2, getRevision());
        ps.setInt(3, ((RelationalReport) getReportFirstSeen()).getDatabaseId());

        FlightPlanType flightPlanType = getFlightPlanType();
        if (flightPlanType != null) {
            ps.setString(4, Character.toString(flightPlanType.getCode()));
        } else {
            ps.setNull(4, Types.CHAR);
        }

        Instant departureTimePlanned = getDepartureTimePlanned();
        if (departureTimePlanned != null) {
            ps.setTimestamp(5, Timestamp.from(departureTimePlanned));
        } else {
            ps.setNull(5, Types.TIMESTAMP);
        }

        ps.setString(6, getRoute());

        int altitudeFeet = getAltitudeFeet();
        if (altitudeFeet >= 0) {
            ps.setInt(7, altitudeFeet);
        } else {
            ps.setNull(7, Types.INTEGER);
        }

        Duration estimatedTimeEnroute = getEstimatedTimeEnroute();
        if (estimatedTimeEnroute != null) {
            ps.setInt(8, (int) estimatedTimeEnroute.toMinutes());
        } else {
            ps.setNull(8, Types.INTEGER);
        }

        Duration estimatedTimeFuel = getEstimatedTimeFuel();
        if (estimatedTimeFuel != null) {
            ps.setInt(9, (int) estimatedTimeFuel.toMinutes());
        } else {
            ps.setNull(9, Types.INTEGER);
        }

        ps.setString(10, getDepartureAirportCode());
        ps.setString(11, getDestinationAirportCode());

        String alternateAirportCode = getAlternateAirportCode();
        if (!alternateAirportCode.isEmpty()) {
            ps.setString(12, alternateAirportCode);
        } else {
            ps.setNull(12, Types.VARCHAR);
        }

        ps.setString(13, getAircraftType());

        ps.executeUpdate();

        ps.close();

        markClean();
    }
}
