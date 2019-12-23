package org.vatplanner.importer.postgis.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.ws.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.importer.postgis.status.entities.RelationalReport;

/**
 * Provides methods to save to and load from a PostGIS database.
 */
public class Database {
    // TODO: use connection pool

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private final String url;
    private final Properties properties;

    private Map<String, Integer> cacheDeduplicationFetchNodes;
    private Map<String, Integer> cacheDeduplicationFetchUrls;

    public Database(DatabaseConfiguration config) {
        url = "jdbc:postgresql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabaseName();

        properties = new Properties();
        properties.setProperty("user", config.getUsername());
        properties.setProperty("password", config.getPassword());
        properties.setProperty("currentSchema", config.getSchema() + ",public");
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection(url, properties);
        } catch (SQLException ex) {
            throw new RuntimeException("failed to connect to database", ex);
        }
    }

    private boolean withConnection(DatabaseAction action) {
        try (Connection connection = connect()) {
            action.run(connection);
            return true;
        } catch (Exception ex) {
            LOGGER.warn("caught exception while connected to DB", ex);
            return false;
        }
    }

    private boolean performTransactional(DatabaseAction action) {
        final Holder<Boolean> success = new Holder<>(true);

        withConnection(connection -> {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException ex) {
                LOGGER.warn("failed to start DB transaction", ex);

                success.value = false;
                return;
            }

            boolean shouldCommit = false;
            try {
                action.run(connection);
                shouldCommit = true;
            } catch (Exception ex) {
                LOGGER.warn("caught exception during DB transaction, rolling back", ex);
                shouldCommit = false;
            }

            if (shouldCommit) {
                try {
                    LOGGER.debug("committing transactional changes to DB");
                    connection.commit();
                } catch (SQLException ex) {
                    LOGGER.warn("commit to DB failed, trying to roll back", ex);
                    shouldCommit = false;
                    success.value = false;
                }
            }

            if (!shouldCommit) {
                try {
                    LOGGER.debug("rolling back DB transaction");
                    connection.rollback();
                    LOGGER.info("DB transaction successfully rolled back");
                } catch (SQLException ex) {
                    LOGGER.warn("DB transaction rollback failed", ex);
                    success.value = false;
                }
            }
        });

        return success.value;
    }

    public Instant getLatestFetchTime() {
        Holder<Instant> latestFetchTime = new Holder<>();

        withConnection(db -> {
            staticQuery(db, "SELECT MAX(fetchtime) AS latestfetchtime FROM reports;", rs -> {
                if (!rs.next()) {
                    return;
                }

                latestFetchTime.value = toInstant(rs.getTimestamp("latestfetchtime"));
            });
        });

        return latestFetchTime.value;
    }

    private void staticQuery(Connection db, String sql, ExceptionalConsumer<ResultSet> resultSetConsumer) throws Exception {
        try (
                Statement stmt = db.createStatement();
                ResultSet rs = stmt.executeQuery(sql);) {
            resultSetConsumer.accept(rs);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return (timestamp != null) ? timestamp.toInstant() : null;
    }

    public void saveDirtyEntities(DirtyEntityTracker tracker) {
        int dirtyBefore = tracker.countDirtyEntities();
        LOGGER.debug("saving {} dirty entities to database", dirtyBefore);

        boolean success = performTransactional(db -> {
            initializeCaches();

            forEach(db, tracker.getDirtyEntities(RelationalReport.class), this::insertReport);

            int dirtyAfter = tracker.countDirtyEntities();
            if (dirtyAfter > 0) {
                LOGGER.error("{} entities remained dirty after saving ({} reported before), ", dirtyAfter, dirtyBefore);
                throw new Exception("unexpected number of dirty entities remaining during sync to database");
            }

            evictCaches();
        });

        if (!success) {
            LOGGER.error("Saving entities to database failed; inconsistent state of graph. Exiting...");
            System.exit(1);
        }
    }

    private void initializeCaches() {
        if ((cacheDeduplicationFetchNodes != null) || (cacheDeduplicationFetchUrls != null)) {
            throw new UnsupportedOperationException("caches must not be reused across transactions");
        }

        cacheDeduplicationFetchNodes = new HashMap<>();
        cacheDeduplicationFetchUrls = new HashMap<>();
    }

    private void evictCaches() {
        cacheDeduplicationFetchNodes = null;
        cacheDeduplicationFetchUrls = null;
    }

    private <T> void forEach(Connection db, Collection<T> elements, ExceptionalBiConsumer<Connection, T> consumer) throws Exception {
        for (T element : elements) {
            consumer.accept(db, element);
        }
    }

    private void insertReport(Connection db, RelationalReport report) throws SQLException {
        if (report.getDatabaseId() > 0) {
            throw new UnsupportedOperationException("updating reports is not implemented");
        }

        LOGGER.trace("INSERT report: record time {}, fetch time {}", report.getRecordTime(), report.getFetchTime());

        // TODO: replace deduplication by DB functions
        int fetchNodeId = getDeduplicationId(
                db,
                cacheDeduplicationFetchNodes,
                report.getFetchNode(),
                "SELECT fetchnode_id FROM fetchnodes WHERE \"name\"=?",
                "INSERT INTO fetchnodes (\"name\") VALUES (?) RETURNING fetchnode_id"
        );

        int fetchUrlRequestedId = getDeduplicationId(
                db,
                cacheDeduplicationFetchUrls,
                report.getFetchUrlRequested(),
                "SELECT fetchurl_id FROM fetchurls WHERE \"url\"=?",
                "INSERT INTO fetchurls (\"url\") VALUES (?) RETURNING fetchurl_id"
        );

        int fetchUrlRetrievedId = getDeduplicationId(
                db,
                cacheDeduplicationFetchUrls,
                report.getFetchUrlRetrieved(),
                "SELECT fetchurl_id FROM fetchurls WHERE \"url\"=?",
                "INSERT INTO fetchurls (\"url\") VALUES (?) RETURNING fetchurl_id"
        );

        // TODO: record number of skipped clients
        PreparedStatement ps = db.prepareStatement("INSERT INTO reports (recordtime, connectedclients, fetchtime, fetchnode_id, fetchurlrequested_id, fetchurlretrieved_id, parsetime, parserrejectedlines) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING report_id");
        ps.setTimestamp(1, Timestamp.from(report.getRecordTime()));
        ps.setInt(2, report.getNumberOfConnectedClients());
        ps.setTimestamp(3, Timestamp.from(report.getFetchTime()));
        if (fetchNodeId > 0) {
            ps.setInt(4, fetchNodeId);
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.setInt(5, fetchUrlRequestedId);
        if (fetchUrlRetrievedId > 0) {
            ps.setInt(6, fetchUrlRetrievedId);
        } else {
            ps.setNull(6, Types.INTEGER);
        }
        ps.setTimestamp(7, Timestamp.from(report.getParseTime()));
        ps.setInt(8, report.getParserRejectedLines());

        ResultSet rs = ps.executeQuery();

        rs.next();
        int reportId = rs.getInt("report_id");

        rs.close();
        ps.close();

        if (reportId <= 0) {
            throw new RuntimeException("unexpected report ID after insert: " + reportId);
        }

        report
                .setDatabaseId(reportId)
                .markClean();
    }

    private int getDeduplicationId(Connection db, Map<String, Integer> cache, String original, String sqlSelect, String sqlInsert) throws SQLException {
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
