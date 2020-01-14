package org.vatplanner.importer.postgis.status.database;

import java.sql.Connection;
import java.sql.SQLException;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightEvent;

public class Caches {

    private DeduplicationCache fetchNodes;
    private DeduplicationCache fetchUrls;
    private StrictEnumCache<FlightEvent> flightEvents;

    public Caches(Connection db) throws SQLException {
        fetchNodes = new DeduplicationCache(
                db,
                "SELECT fetchnode_id FROM fetchnodes WHERE \"name\"=?",
                "INSERT INTO fetchnodes (\"name\") VALUES (?) RETURNING fetchnode_id"
        );

        fetchUrls = new DeduplicationCache(
                db,
                "SELECT fetchurl_id FROM fetchurls WHERE \"url\"=?",
                "INSERT INTO fetchurls (\"url\") VALUES (?) RETURNING fetchurl_id"
        );

        flightEvents = new StrictEnumCache<>(
                db,
                "SELECT flightevent_id, eventname FROM flightevents",
                name -> FlightEvent.valueOf(name.toUpperCase())
        );
    }

    public void evict() {
        fetchNodes = null;
        fetchUrls = null;
        flightEvents = null;
    }

    public DeduplicationCache getFetchNodes() {
        return fetchNodes;
    }

    public DeduplicationCache getFetchUrls() {
        return fetchUrls;
    }

    public StrictEnumCache<FlightEvent> getFlightEvents() {
        return flightEvents;
    }

}
