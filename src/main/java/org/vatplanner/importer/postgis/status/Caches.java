package org.vatplanner.importer.postgis.status;

import java.sql.Connection;

public class Caches {

    private DeduplicationCache fetchNodes;
    private DeduplicationCache fetchUrls;

    public Caches(Connection db) {
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
    }

    public void evict() {
        fetchNodes = null;
        fetchUrls = null;
    }

    public DeduplicationCache getFetchNodes() {
        return fetchNodes;
    }

    public DeduplicationCache getFetchUrls() {
        return fetchUrls;
    }

}
