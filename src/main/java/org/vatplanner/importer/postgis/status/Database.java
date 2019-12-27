package org.vatplanner.importer.postgis.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Properties;
import javax.xml.ws.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.importer.postgis.status.entities.RelationalConnection;
import org.vatplanner.importer.postgis.status.entities.RelationalFacility;
import org.vatplanner.importer.postgis.status.entities.RelationalFlight;
import org.vatplanner.importer.postgis.status.entities.RelationalFlightPlan;
import org.vatplanner.importer.postgis.status.entities.RelationalReport;
import org.vatplanner.importer.postgis.status.entities.RelationalTrackPoint;

/**
 * Provides methods to save to and load from a PostGIS database.
 */
public class Database {
    // TODO: use connection pool

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private final String url;
    private final Properties properties;

    private Caches caches;

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
                }

                success.value = false;
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
            initializeCaches(db);

            forEachWithCaches(db, tracker.getDirtyEntities(RelationalReport.class), RelationalReport::insert);
            forEach(db, tracker.getDirtyEntities(RelationalConnection.class), RelationalConnection::upsert);
            forEach(db, tracker.getDirtyEntities(RelationalFacility.class), RelationalFacility::insert);
            forEach(db, tracker.getDirtyEntities(RelationalFlight.class), RelationalFlight::insert);
            forEach(db, tracker.getDirtyEntities(RelationalFlightPlan.class), RelationalFlightPlan::insert);
            forEach(db, tracker.getDirtyEntities(RelationalTrackPoint.class), RelationalTrackPoint::insert);

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

    private void initializeCaches(Connection db) {
        if (caches != null) {
            throw new UnsupportedOperationException("caches must not be reused across transactions");
        }

        caches = new Caches(db);
    }

    private void evictCaches() {
        caches.evict();
        caches = null;
    }

    private <T> void forEach(Connection db, Collection<T> elements, ExceptionalBiConsumer<T, Connection> consumer) throws Exception {
        for (T element : elements) {
            consumer.accept(element, db);
        }
    }

    private <T> void forEachWithCaches(Connection db, Collection<T> elements, ExceptionalTriConsumer<T, Connection, Caches> consumer) throws Exception {
        for (T element : elements) {
            consumer.accept(element, db, caches);
        }
    }

}
