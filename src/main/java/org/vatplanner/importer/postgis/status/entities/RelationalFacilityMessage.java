package org.vatplanner.importer.postgis.status.entities;

import org.vatplanner.dataformats.vatsimpublic.entities.status.Facility;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FacilityMessage;

/**
 * {@link FacilityMessage} extended for exchange with PostGIS.
 */
public class RelationalFacilityMessage extends FacilityMessage {
    // FIXME: implement

    public RelationalFacilityMessage(Facility facility) {
        super(facility);
    }

}
