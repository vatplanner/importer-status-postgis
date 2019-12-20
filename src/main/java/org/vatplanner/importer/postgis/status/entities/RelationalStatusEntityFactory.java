package org.vatplanner.importer.postgis.status.entities;

import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Connection;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Facility;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FacilityMessage;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlan;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.StatusEntityFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.TrackPoint;

/**
 * Factory producing graph entities extended to be exchanged with PostGIS.
 */
public class RelationalStatusEntityFactory implements StatusEntityFactory {

    @Override
    public Connection createConnection(Member member, Instant logonTime) {
        return new RelationalConnection(member, logonTime);
    }

    @Override
    public Facility createFacility(String name) {
        return new RelationalFacility(name);
    }

    @Override
    public FacilityMessage createFacilityMessage(Facility facility) {
        return new RelationalFacilityMessage(facility);
    }

    @Override
    public Flight createFlight(Member member, String callsign) {
        return new RelationalFlight(member, callsign);
    }

    @Override
    public FlightPlan createFlightPlan(Flight flight, int revision) {
        return new RelationalFlightPlan(flight, revision);
    }

    @Override
    public Member createMember(int vatsimId) {
        return new RelationalMember(vatsimId);
    }

    @Override
    public Report createReport(Instant recordTime) {
        return new RelationalReport(recordTime);
    }

    @Override
    public TrackPoint createTrackPoint(Report report) {
        return new RelationalTrackPoint(report);
    }

}
