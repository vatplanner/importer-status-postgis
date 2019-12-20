package org.vatplanner.importer.postgis.status.entities;

import java.time.Instant;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Connection;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;

/**
 * {@link Connection} extended for exchange with PostGIS.
 */
public class RelationalConnection extends Connection {
    // FIXME: implement

    public RelationalConnection(Member member, Instant logonTime) {
        super(member, logonTime);
    }

}
