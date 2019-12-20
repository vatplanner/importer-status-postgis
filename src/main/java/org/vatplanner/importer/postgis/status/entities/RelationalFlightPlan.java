package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlan;

/**
 * {@link FlightPlan} extended for exchange with PostGIS.
 */
public class RelationalFlightPlan extends FlightPlan {
    // FIXME: implement

    public RelationalFlightPlan(Flight flight, int revision) {
        super(flight, revision);
    }

}
