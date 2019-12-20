package org.vatplanner.importer.postgis.status.entities;

import java.time.Duration;
import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.CommunicationMode;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlan;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlanType;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.SimpleEquipmentSpecification;
import org.vatplanner.dataformats.vatsimpublic.entities.status.WakeTurbulenceCategory;

/**
 * {@link FlightPlan} extended for exchange with PostGIS.
 */
public class RelationalFlightPlan extends FlightPlan implements DirtyMark {

    private boolean isDirty = true;
    private int databaseId = -1;

    public RelationalFlightPlan(Flight flight, int revision) {
        super(flight, revision);
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
