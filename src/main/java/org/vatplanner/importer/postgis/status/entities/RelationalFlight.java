package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Flight;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;

/**
 * {@link Flight} extended for exchange with PostGIS.
 */
public class RelationalFlight extends Flight {
    // FIXME: implement

    public RelationalFlight(Member member, String callsign) {
        super(member, callsign);
    }

}
