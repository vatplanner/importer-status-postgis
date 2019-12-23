package org.vatplanner.importer.postgis.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeduplicationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeduplicationCache.class);

    private final Map<String, Integer> cache = new HashMap<>();
    private final Connection db;
    private final String sqlSelect;
    private final String sqlInsert;

    public DeduplicationCache(Connection db, String sqlSelect, String sqlInsert) {
        this.db = db;
        this.sqlSelect = sqlSelect;
        this.sqlInsert = sqlInsert;
    }

    public int getId(String original) throws SQLException {
        // TODO: replace by DB function

        if (original == null) {
            LOGGER.trace("DEDUPLICATION NULL, no lookup");
            return 0;
        }

        int id = cache.getOrDefault(original, -1);

        if (id > 0) {
            LOGGER.trace("DEDUPLICATION CACHED: {} => {}", original, id);
            return id;
        }

        LOGGER.trace("DEDUPLICATION SELECT for {}: ", original, sqlSelect);

        PreparedStatement ps = db.prepareStatement(sqlSelect);
        ps.setString(1, original);

        ResultSet rs = ps.executeQuery();
        boolean hasId = rs.next();
        if (hasId) {
            id = rs.getInt(1);
        } else {
            rs.close();
            ps.close();

            LOGGER.trace("DEDUPLICATION INSERT for {}: ", original, sqlSelect);

            ps = db.prepareStatement(sqlInsert);
            ps.setString(1, original);
            rs = ps.executeQuery();

            rs.next();
            id = rs.getInt(1);
        }

        rs.close();
        ps.close();

        LOGGER.trace("DEDUPLICATION FOUND: {} => {}", original, id);

        cache.put(original, id);

        return id;
    }

}
