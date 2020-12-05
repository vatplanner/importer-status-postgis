package org.vatplanner.importer.postgis.status.database;

import static org.vatplanner.importer.postgis.status.utils.TimeHelpers.isBetween;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.newsclub.net.unix.socketfactory.PostgresqlAFUNIXSocketFactory;
import org.postgresql.util.PGInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.BarometricPressure;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FacilityType;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightEvent;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlan;
import org.vatplanner.dataformats.vatsimpublic.entities.status.FlightPlanType;
import org.vatplanner.dataformats.vatsimpublic.entities.status.GeoCoordinates;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Member;
import org.vatplanner.dataformats.vatsimpublic.entities.status.Report;
import org.vatplanner.dataformats.vatsimpublic.entities.status.StatusEntityFactory;
import org.vatplanner.dataformats.vatsimpublic.graph.GraphIndex;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;
import org.vatplanner.importer.postgis.status.configuration.DatabaseConfiguration;
import org.vatplanner.importer.postgis.status.entities.RelationalConnection;
import org.vatplanner.importer.postgis.status.entities.RelationalFacility;
import org.vatplanner.importer.postgis.status.entities.RelationalFlight;
import org.vatplanner.importer.postgis.status.entities.RelationalFlightPlan;
import org.vatplanner.importer.postgis.status.entities.RelationalReport;
import org.vatplanner.importer.postgis.status.entities.RelationalTrackPoint;
import org.vatplanner.importer.postgis.status.utils.ExceptionalBiConsumer;
import org.vatplanner.importer.postgis.status.utils.ExceptionalConsumer;
import org.vatplanner.importer.postgis.status.utils.ExceptionalRunnable;
import org.vatplanner.importer.postgis.status.utils.ExceptionalTriConsumer;
import org.vatplanner.importer.postgis.status.utils.Holder;
import org.vatplanner.importer.postgis.status.utils.TimeHelpers;

/**
 * Provides methods to save to and load from a PostGIS database.
 */
public class Database {
    // TODO: use connection pool

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private final String url;
    private final Properties properties;

    private Caches caches;

    private static final FacilityType DUMMY_FACILITY_TYPE = FacilityType.CENTER;
    private static final int DUMMY_FACILITY_FREQUENCY_KILOHERTZ = 120000;

    private static final String SUB_PATTERN_DOUBLE = "(-?[0-9]+(?:\\.[0-9]+|))";
    private static final Pattern PATTERN_POSTGIS_POINTZ = Pattern.compile(
        "^POINT Z \\(" + SUB_PATTERN_DOUBLE + " " + SUB_PATTERN_DOUBLE + " " + SUB_PATTERN_DOUBLE + "\\)$" //
    );
    private static final int PATTERN_POSTGIS_POINTZ_LONGITUDE = 1;
    private static final int PATTERN_POSTGIS_POINTZ_LATITUDE = 2;
    private static final int PATTERN_POSTGIS_POINTZ_Z = 3;

    private static final Duration FLIGHT_PLAN_RETENTION_TIME = Duration.ofHours(2); // TODO: make configurable

    public Database(DatabaseConfiguration config) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("jdbc:postgresql://");
        urlBuilder.append(config.getHost());
        urlBuilder.append(":");
        urlBuilder.append(config.getPort());
        urlBuilder.append("/");
        urlBuilder.append(config.getDatabaseName());

        if (config.hasUnixSocketPath()) {
            urlBuilder.append("?socketFactory=");
            urlBuilder.append(PostgresqlAFUNIXSocketFactory.class.getCanonicalName());
            urlBuilder.append("&socketFactoryArg=");
            urlBuilder.append(config.getUnixSocketPath());
        }

        url = urlBuilder.toString();

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
            query(db, "SELECT MAX(fetchtime) AS latestfetchtime FROM reports;", rs -> {
                if (!rs.next()) {
                    return;
                }

                latestFetchTime.value = toInstant(rs.getTimestamp("latestfetchtime"));
            });
        });

        return latestFetchTime.value;
    }

    private void query(Connection db, String sql, ExceptionalConsumer<ResultSet, Exception> resultSetConsumer) throws Exception {
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
            forEachWithCaches(db, tracker.getDirtyEntities(RelationalFlight.class), RelationalFlight::insert);
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

    private void initializeCaches(Connection db) throws SQLException {
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

    public void loadReportsSinceRecordTime(GraphIndex graphIndex, StatusEntityFactory statusEntityFactory, Instant earliestRecordTimestamp) {
        LOGGER.debug("loading reports starting at record time {} from database", earliestRecordTimestamp);

        boolean success = performTransactional(db -> {
            Instant start = Instant.now();

            // While there usually should be no concurrent transaction aside
            // from current instance of this application, better make sure we
            // don't read anything inconsistent as it's not guaranteed otherwise
            // and mixing up data would yield puzzling hard to explain permanent
            // errors in imported data.
            execute(db, "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");

            initializeCaches(db);

            // Create temporary tables to pre-select rows to be loaded.
            execute(db, ""
                + "CREATE TEMPORARY TABLE _load_reports ( "
                + "    report_id INT, "
                + "    complete BOOL, "
                + "    PRIMARY KEY ( report_id ) "
                + ") ");

            execute(db, ""
                + "CREATE TEMPORARY TABLE _load_connections ( "
                + "    connection_id INT, "
                + "    PRIMARY KEY ( connection_id ) "
                + ") ");

            execute(db, ""
                + "CREATE TEMPORARY TABLE _load_flights ( "
                + "    flight_id INT, "
                + "    PRIMARY KEY ( flight_id ) "
                + ") ");

            // select reports to load completely
            // based on fetch time
            executeBenchmarked("PRESELECT reports (complete) / fetch time", db, ""
                + "INSERT INTO _load_reports "
                + "SELECT report_id, true "
                + "FROM reports "
                + "WHERE fetchtime >= ? ",
                ps -> ps.setTimestamp(1, Timestamp.from(earliestRecordTimestamp)));

            // select connections to load
            // connections within record time span of selected complete reports
            executeBenchmarked("PRESELECT connections / complete reports", db, ""
                + "INSERT INTO _load_connections "
                + "SELECT c.connection_id "
                + "FROM connections c "
                + "LEFT OUTER JOIN reports rf ON c.firstreport_id = rf.report_id "
                + "LEFT OUTER JOIN reports rl ON c.lastreport_id = rl.report_id "
                + "WHERE (rf.recordtime, rl.recordtime) OVERLAPS ( "
                + "   (SELECT MIN(r.recordtime) "
                + "    FROM _load_reports _lr "
                + "    LEFT OUTER JOIN reports r ON r.report_id = _lr.report_id "
                + "    WHERE _lr.complete = true "
                + "	  ), "
                + "   (SELECT MAX(r.recordtime) + INTERVAL '1 second' "
                + "	   FROM _load_reports _lr "
                + "	   LEFT OUTER JOIN reports r ON r.report_id = _lr.report_id "
                + "    WHERE _lr.complete = true "
                + "   ) "
                + ") ");

            // select flights to load
            // referenced by one or more connections
            executeBenchmarked("PRESELECT flights / connections", db, ""
                + "INSERT INTO _load_flights "
                + "SELECT DISTINCT flight_id "
                + "FROM connections_flights cf "
                + "WHERE cf.connection_id IN (SELECT connection_id FROM _load_connections) ");

            // select flights to load
            // referenced by flight plans
            // with maximum theoretical (not actual) retention time
            // within timespan of reports preselected for complete import
            executeBenchmarked(
                "PRESELECT flights / flight plans / complete reports within maximum retention time "
                    + FLIGHT_PLAN_RETENTION_TIME,
                db, ""
                    + "INSERT INTO _load_flights "
                    + "SELECT DISTINCT flight_id "
                    + "FROM flightplans fp "
                    + "LEFT OUTER JOIN reports rfs ON rfs.report_id = fp.firstseen_report_id "
                    + "WHERE (rfs.recordtime, rfs.recordtime + ?) OVERLAPS ( "
                    + "   (SELECT MIN(r.recordtime) "
                    + "    FROM _load_reports _lr "
                    + "    LEFT OUTER JOIN reports r ON r.report_id = _lr.report_id "
                    + "    WHERE _lr.complete = true "
                    + "   ), "
                    + "   (SELECT MAX(r.recordtime) + INTERVAL '1 second' "
                    + "    FROM _load_reports _lr "
                    + "    LEFT OUTER JOIN reports r ON r.report_id = _lr.report_id "
                    + "    WHERE _lr.complete = true "
                    + "   ) "
                    + ") "
                    + "ON CONFLICT DO NOTHING ",
                ps -> ps.setObject(1, toPostgresInterval(FLIGHT_PLAN_RETENTION_TIME)));

            // select additional connections to load
            // referenced by selected flights
            // (this is required because selecting flights by connections adds
            // new dependencies back to even more connections)
            executeBenchmarked("PRESELECT connections / flights", db, ""
                + "INSERT INTO _load_connections "
                + "SELECT connection_id "
                + "FROM connections_flights cf "
                + "WHERE cf.flight_id IN (SELECT flight_id FROM _load_flights) "
                + "ON CONFLICT DO NOTHING ");

            // select additional reports to load partially
            // referenced by selected connections
            executeBenchmarked("PRESELECT reports (incomplete) / connections, first report", db, ""
                + "INSERT INTO _load_reports "
                + "SELECT c.firstreport_id, false "
                + "FROM _load_connections _lc "
                + "LEFT OUTER JOIN connections c ON _lc.connection_id = c.connection_id "
                + "ON CONFLICT DO NOTHING ");

            executeBenchmarked("PRESELECT reports (incomplete) / connections, last report", db, ""
                + "INSERT INTO _load_reports "
                + "SELECT c.lastreport_id, false "
                + "FROM _load_connections _lc "
                + "LEFT OUTER JOIN connections c ON _lc.connection_id = c.connection_id "
                + "ON CONFLICT DO NOTHING ");

            // select additional reports to load partially
            // referenced by flight plans
            // referenced by selected flights
            executeBenchmarked("PRESELECT reports (incomplete) / flight plans / flights", db, ""
                + "INSERT INTO _load_reports "
                + "SELECT firstseen_report_id, false "
                + "FROM flightplans fp "
                + "LEFT OUTER JOIN flights f ON f.flight_id = fp.flight_id "
                + "WHERE fp.flight_id IN (SELECT flight_id FROM _load_flights) "
                + "ON CONFLICT DO NOTHING ");

            // select additional reports to load partially
            // referenced by trackpoints
            // referenced by selected flights
            executeBenchmarked("PRESELECT reports (incomplete) / trackpoints / flights", db, ""
                + "INSERT INTO _load_reports "
                + "SELECT report_id, false "
                + "FROM trackpoints tp "
                + "LEFT OUTER JOIN flights f ON f.flight_id = tp.flight_id "
                + "WHERE tp.flight_id IN (SELECT flight_id FROM _load_flights) "
                + "ON CONFLICT DO NOTHING ");

            Instant endPreselect = Instant.now();

            // read all preselected reports
            Map<Integer, RelationalReport> reportsById = new HashMap<>();
            query(db, ""
                + "SELECT r.report_id, recordtime, connectedclients, fetchtime, fureq.url fetchurlrequested, furet.url fetchurlretrieved, fn.name fetchnode, parsetime, parserrejectedlines "
                + "FROM _load_reports _lr "
                + "LEFT OUTER JOIN reports r ON r.report_id = _lr.report_id "
                + "LEFT OUTER JOIN fetchurls fureq ON r.fetchurlrequested_id = fureq.fetchurl_id "
                + "LEFT OUTER JOIN fetchurls furet ON r.fetchurlretrieved_id = fureq.fetchurl_id "
                + "LEFT OUTER JOIN fetchnodes fn ON r.fetchnode_id = fn.fetchnode_id ",
                rs -> {
                    while (rs.next()) {
                        int reportId = rs.getInt("report_id");

                        RelationalReport report = (RelationalReport) statusEntityFactory
                            .createReport(rs.getTimestamp("recordtime").toInstant());
                        report.setDatabaseId(reportId);
                        report.setFetchNode(rs.getString("fetchnode"));
                        report.setFetchTime(rs.getTimestamp("fetchtime").toInstant());
                        report.setFetchUrlRequested(rs.getString("fetchurlrequested"));
                        report.setFetchUrlRetrieved(rs.getString("fetchurlretrieved"));
                        report.setNumberOfConnectedClients(rs.getInt("connectedclients"));
                        report.setParseTime(rs.getTimestamp("parsetime").toInstant());
                        report.setParserRejectedLines(rs.getInt("parserrejectedlines"));
                        report.markClean();

                        if (reportsById.put(reportId, report) != null) {
                            throw new RuntimeException("duplicate report ID " + reportId);
                        }
                    }

                    LOGGER.debug("read {} reports from database", reportsById.size());
                });

            // read all preselected connections
            Map<Integer, Member> membersByVatsimId = new HashMap<>();
            Map<Integer, RelationalConnection> connectionsById = new HashMap<>();
            query(db, ""
                + "SELECT c.connection_id, logontime, vatsimid, firstreport_id, lastreport_id "
                + "FROM _load_connections _lc "
                + "LEFT OUTER JOIN connections c ON c.connection_id = _lc.connection_id ",
                rs -> {
                    while (rs.next()) {
                        int connectionId = rs.getInt("connection_id");

                        Member member = membersByVatsimId.computeIfAbsent(rs.getInt("vatsimid"),
                            statusEntityFactory::createMember);

                        RelationalConnection connection = (RelationalConnection) statusEntityFactory.createConnection(
                            member,
                            rs.getTimestamp("logontime").toInstant());
                        connection.setDatabaseId(connectionId);

                        int firstReportId = rs.getInt("firstreport_id");
                        RelationalReport firstReport = reportsById.get(firstReportId);
                        if (firstReport == null) {
                            throw new RuntimeException(
                                "connection ID " + connectionId
                                    + ": report (first) with ID " + firstReportId + " has not been loaded" //
                            );
                        }
                        connection.seenInReport(firstReport);

                        int lastReportId = rs.getInt("lastreport_id");
                        RelationalReport lastReport = reportsById.get(lastReportId);
                        if (lastReport == null) {
                            throw new RuntimeException(
                                "connection ID " + connectionId
                                    + ": report (last) with ID " + lastReportId + " has not been loaded" //
                            );
                        }
                        connection.seenInReport(lastReport);

                        connection.markClean();

                        if (connectionsById.put(connectionId, connection) != null) {
                            throw new RuntimeException("duplicate connection ID " + connectionId);
                        }
                    }

                    LOGGER.debug("read {} connections from database", connectionsById.size());
                    LOGGER.debug(
                        "read {} members (combined total as of reading connections) from database",
                        membersByVatsimId.size() //
                    );
                });

            // read all facilities of preselected connections
            query(db, ""
                + "SELECT connection_id, name "
                + "FROM facilities "
                + "WHERE connection_id IN (SELECT connection_id FROM _load_connections) ",
                rs -> {
                    int importedFacilities = 0;
                    while (rs.next()) {
                        importedFacilities++;
                        int connectionId = rs.getInt("connection_id");

                        RelationalConnection connection = connectionsById.get(connectionId);
                        if (connection == null) {
                            throw new RuntimeException(
                                "facility: connection ID " + connectionId + " has not been read from database" //
                            );
                        }

                        RelationalFacility facility = (RelationalFacility) statusEntityFactory.createFacility(
                            rs.getString("name") //
                        );
                        facility.setHasRecordInDatabase(true);
                        facility.setConnection(connection);

                        // FIXME: it may actually be required to store the actual values in DB although
                        // irrelevant after import, check graph import match logic
                        facility.setType(DUMMY_FACILITY_TYPE);
                        facility.seenOnFrequencyKilohertz(DUMMY_FACILITY_FREQUENCY_KILOHERTZ);

                        facility.markClean();

                        // since we only imported ATC providing facilities,
                        // expect evaluation after import to indicate the
                        // same state
                        if (!facility.providesATCService()) {
                            throw new RuntimeException(
                                "facility for connection ID " + connectionId
                                    + " is not indicating ATC service after import" //
                            );
                        }

                        // facilities are linked on members
                        connection.getMember().addFacility(facility);

                        // facilities are linked on reports
                        // disconnecting from VATSIM terminates facilities,
                        // so record time is sufficient to reconstruct all
                        // reports
                        Instant firstRecordTime = connection.getFirstReport().getRecordTime();
                        Instant lastRecordTime = connection.getLastReport().getRecordTime();
                        reportsById
                            .values()
                            .stream()
                            .filter(report -> isBetween(report.getRecordTime(), firstRecordTime, lastRecordTime))
                            .forEach(report -> report.addFacility(facility));
                    }

                    LOGGER.debug("read {} facilities from database", importedFacilities);
                });

            // read all preselected flights
            Map<Integer, RelationalFlight> flightsById = new HashMap<>();
            query(db, ""
                + "SELECT f.flight_id, vatsimid, callsign "
                + "FROM _load_flights _lf "
                + "LEFT OUTER JOIN flights f ON f.flight_id = _lf.flight_id ",
                rs -> {
                    while (rs.next()) {
                        int flightId = rs.getInt("flight_id");

                        Member member = membersByVatsimId.computeIfAbsent(
                            rs.getInt("vatsimid"),
                            statusEntityFactory::createMember //
                        );

                        RelationalFlight flight = (RelationalFlight) statusEntityFactory.createFlight(
                            member,
                            rs.getString("callsign") //
                        );
                        flight.setDatabaseId(flightId);
                        flight.markClean();

                        // flights are linked on members
                        member.addFlight(flight);

                        if (flightsById.put(flightId, flight) != null) {
                            throw new RuntimeException("duplicate flight ID " + flightId);
                        }
                    }

                    LOGGER.debug("read {} flights from database", flightsById.size());
                });

            // associate all preselected flights with connections
            query(db, ""
                + "SELECT flight_id, connection_id "
                + "FROM connections_flights cf "
                + "WHERE flight_id IN (SELECT flight_id FROM _load_flights) ",
                rs -> {
                    int numAssociations = 0;
                    while (rs.next()) {
                        numAssociations++;
                        int flightId = rs.getInt("flight_id");
                        int connectionId = rs.getInt("connection_id");

                        RelationalFlight flight = flightsById.get(flightId);
                        if (flight == null) {
                            throw new RuntimeException(
                                "flight ID " + flightId
                                    + " (for association with connection ID " + connectionId
                                    + ") has not been loaded" //
                            );
                        }

                        RelationalConnection connection = connectionsById.get(connectionId);
                        if (connection == null) {
                            throw new RuntimeException(
                                "connection ID " + connectionId
                                    + " (for association with flight ID " + flightId
                                    + ") has not been loaded" //
                            );
                        }

                        flight.addConnection(connection);
                        flight.markClean();
                    }

                    LOGGER.debug("created {} associations between flights and connections", numAssociations);
                });

            // read all flight plans of preselected flights
            query(db, ""
                + "SELECT flight_id, revision, firstseen_report_id, flightplantype, route, altitudefeet, minutesenroute, minutesfuel, departureairport, destinationairport, alternateairport, aircrafttype, departuretimeplanned "
                + "FROM flightplans fp "
                + "WHERE flight_id IN (SELECT flight_id FROM _load_flights)",
                rs -> {
                    int numFlightPlans = 0;
                    while (rs.next()) {
                        numFlightPlans++;
                        int flightId = rs.getInt("flight_id");
                        int revision = rs.getInt("revision");

                        RelationalFlight flight = flightsById.get(flightId);
                        if (flight == null) {
                            throw new RuntimeException("flight ID " + flightId + " has not been loaded");
                        }

                        RelationalFlightPlan flightPlan = (RelationalFlightPlan) statusEntityFactory
                            .createFlightPlan(flight, revision);
                        flightPlan.setAircraftType(rs.getString("aircrafttype"));
                        flightPlan.setAlternateAirportCode(rs.getString("alternateairport"));
                        flightPlan.setAltitudeFeet(negativeIfNull(rs, "altitudefeet"));
                        flightPlan.setDepartureAirportCode(rs.getString("departureairport"));
                        flightPlan.setDepartureTimePlanned(toInstant(rs.getTimestamp("departuretimeplanned")));
                        flightPlan.setDestinationAirportCode(rs.getString("destinationairport"));
                        flightPlan.setEstimatedTimeEnroute(nullableDurationOfMinutes(rs, "minutesenroute"));
                        flightPlan.setEstimatedTimeFuel(nullableDurationOfMinutes(rs, "minutesfuel"));
                        flightPlan.setFlightPlanType(
                            FlightPlanType.resolveFlightPlanCode(rs.getString("flightplantype")) //
                        );
                        flightPlan.setRoute(rs.getString("route"));

                        int firstSeenReportId = rs.getInt("firstseen_report_id");
                        RelationalReport firstSeenReport = reportsById.get(firstSeenReportId);
                        if (firstSeenReport == null) {
                            throw new RuntimeException("report ID " + firstSeenReportId + " has not been loaded");
                        }

                        flightPlan.seenInReport(firstSeenReport);

                        flightPlan.markClean();

                        flight.addFlightPlan(flightPlan);
                    }

                    LOGGER.debug("read {} flight plans from database", numFlightPlans);
                });

            // read all track points of preselected flights
            query(db, ""
                + "SELECT tp.flight_id, tp.report_id, ST_AsText(geocoords) geocoords, heading, groundspeed, transpondercode, qnhcinhg, flightevent_id "
                + "FROM trackpoints tp "
                + "LEFT OUTER JOIN trackpoints_flightevents tpfe ON tpfe.flight_id = tp.flight_id AND tpfe.report_id = tp.report_id "
                + "WHERE tp.flight_id IN (SELECT flight_id FROM _load_flights) ",
                rs -> {
                    int numTrackPoints = 0;
                    int numMarkedEvents = 0;
                    while (rs.next()) {
                        numTrackPoints++;

                        int flightId = rs.getInt("flight_id");
                        RelationalFlight flight = flightsById.get(flightId);
                        if (flight == null) {
                            throw new RuntimeException("flight ID " + flightId + " has not been loaded");
                        }

                        int reportId = rs.getInt("report_id");
                        RelationalReport report = reportsById.get(reportId);
                        if (report == null) {
                            throw new RuntimeException("report ID " + reportId + " has not been loaded");
                        }

                        RelationalTrackPoint trackPoint = (RelationalTrackPoint) statusEntityFactory
                            .createTrackPoint(report);
                        trackPoint.setFlight(flight);
                        trackPoint.setGeoCoordinates(convertPostGisToGeoCoordinates(rs.getString("geocoords")));
                        trackPoint.setGroundSpeed(negativeIfNull(rs, "groundspeed"));
                        trackPoint.setHeading(negativeIfNull(rs, "heading"));
                        trackPoint.setQnh(nullableBarometricPressureFromCentiInchesOfMercury(rs, "qnhcinhg"));
                        trackPoint.setTransponderCode(negativeIfNull(rs, "transpondercode"));
                        trackPoint.markClean();

                        flight.addTrackPoint(trackPoint);

                        report.addFlight(flight);

                        FlightEvent event = nullableFlightEvent(rs, "flightevent_id");
                        if (event != null) {
                            if (flight.isDirty()) {
                                throw new RuntimeException(
                                    "flight " + flight.getDatabaseId()
                                        + " is marked dirty, unable to import flightevent" //
                                );
                            }

                            flight.markEvent(trackPoint, event);
                            flight.markClean();

                            numMarkedEvents++;
                        }
                    }

                    LOGGER.debug(
                        "read {} track points from database, marked {} events",
                        numTrackPoints, numMarkedEvents //
                    );
                });

            // register all flights to reports as indicated by record times on
            // connections
            Instant startFlightConnectionRegistration = Instant.now();
            for (RelationalFlight flight : flightsById.values()) {
                for (org.vatplanner.dataformats.vatsimpublic.entities.status.Connection connection : flight
                    .getConnections()) {
                    Instant firstRecordTime = connection.getFirstReport().getRecordTime();
                    Instant lastRecordTime = connection.getLastReport().getRecordTime();
                    reportsById
                        .values()
                        .stream()
                        .filter(report -> isBetween(report.getRecordTime(), firstRecordTime, lastRecordTime))
                        .forEach(report -> report.addFlight(flight));
                }
            }
            Instant endFlightConnectionRegistration = Instant.now();
            LOGGER.debug(
                "registered loaded flights to reports by connections (took {}ms)",
                Duration.between(startFlightConnectionRegistration, endFlightConnectionRegistration).toMillis() //
            );

            // register flights to reports by flight plans (for prefilings)
            // starting with first seen report of flight plan and assuming
            // flight was visible for the usual retention time; this does not
            // provide an accurate but only a plausible reconstruction of data
            Instant startFlightPlanRegistration = Instant.now();
            int numNoFlightPlan = 0;
            int numNoPrefiling = 0;
            int numFlightsRegisteredByFlightPlan = 0;
            for (RelationalFlight flight : flightsById.values()) {
                SortedSet<FlightPlan> flightPlans = flight.getFlightPlans();

                // skip flights without any flight plans at all
                if (flightPlans.isEmpty()) {
                    numNoFlightPlan++;
                    continue;
                }

                SortedSet<org.vatplanner.dataformats.vatsimpublic.entities.status.Connection> connections = flight
                    .getConnections();

                Instant firstFlightPlanRevisionRecordTime = getFirst(flightPlans)
                    .map(FlightPlan::getReportFirstSeen)
                    .map(Report::getRecordTime)
                    .orElse(null);

                Instant firstConnectionRecordTime = getFirst(connections)
                    .map(org.vatplanner.dataformats.vatsimpublic.entities.status.Connection::getFirstReport)
                    .map(Report::getRecordTime)
                    .orElse(null);

                Optional<FlightPlan> lastFlightPlanRevisionBeforeConnected;
                if (firstConnectionRecordTime == null) {
                    lastFlightPlanRevisionBeforeConnected = getLast(flightPlans);
                } else {
                    lastFlightPlanRevisionBeforeConnected = flightPlans
                        .stream()
                        .filter( //
                            flightPlan -> flightPlan
                                .getReportFirstSeen()
                                .getRecordTime()
                                .isBefore(firstConnectionRecordTime))
                        .max(Comparator.comparingInt(FlightPlan::getRevision));
                }

                // flights may have no pre-filings but file only while online
                // in which case we don't have revisions before client is
                // connected => skip
                if (!lastFlightPlanRevisionBeforeConnected.isPresent()) {
                    numNoPrefiling++;
                    continue;
                }

                Instant lastFlightPlanRevisionRecordTimeBeforeConnected = lastFlightPlanRevisionBeforeConnected
                    .map(FlightPlan::getReportFirstSeen)
                    .map(Report::getRecordTime)
                    .orElse(null);

                Instant lastConnectionRecordTime = getLast(connections)
                    .map(org.vatplanner.dataformats.vatsimpublic.entities.status.Connection::getLastReport)
                    .map(Report::getRecordTime)
                    .orElse(null);

                // retention of flight plans starts with first revision and is
                // assumed to end with either disconnect or at end of well-known
                // retention period
                Instant latestAssumedRetentionTime = TimeHelpers.min(
                    lastConnectionRecordTime,
                    lastFlightPlanRevisionRecordTimeBeforeConnected.plus(FLIGHT_PLAN_RETENTION_TIME));

                reportsById
                    .values()
                    .stream()
                    .filter(
                        report -> isBetween(
                            report.getRecordTime(),
                            firstFlightPlanRevisionRecordTime,
                            latestAssumedRetentionTime))
                    .forEach(report -> report.addFlight(flight));

                numFlightsRegisteredByFlightPlan++;
            }
            Instant endFlightPlanRegistration = Instant.now();
            LOGGER.debug(
                "registered loaded flights to reports by flight plans/prefilings (took {}ms; {} flights registered, {} without prefiling, {} without flight plan)",
                Duration.between(startFlightPlanRegistration, endFlightPlanRegistration).toMillis(),
                numFlightsRegisteredByFlightPlan,
                numNoPrefiling,
                numNoFlightPlan //
            );

            // delete temporary tables
            execute(db, "DROP TABLE _load_connections");
            execute(db, "DROP TABLE _load_flights");
            execute(db, "DROP TABLE _load_reports");

            // insert entities to graph index
            membersByVatsimId.values().forEach(graphIndex::add);
            reportsById.values().forEach(graphIndex::add);

            Instant end = Instant.now();
            LOGGER.info(
                "Loading complete after {}ms (preselect {}ms, fetch {}ms)",
                Duration.between(start, end).toMillis(),
                Duration.between(start, endPreselect).toMillis(),
                Duration.between(endPreselect, end).toMillis() //
            );

            evictCaches();
        });

        if (!success) {
            LOGGER.error("Failed to load reports from database, giving up...");
            System.exit(1);
        }
    }

    private <T> Optional<T> getFirst(SortedSet<T> set) {
        if (set.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(set.first());
    }

    private <T> Optional<T> getLast(SortedSet<T> set) {
        if (set.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(set.last());
    }

    private GeoCoordinates convertPostGisToGeoCoordinates(String s) {
        Matcher matcher = PATTERN_POSTGIS_POINTZ.matcher(s);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported input format: \"" + s + "\"");
        }

        double latitude = Double.parseDouble(matcher.group(PATTERN_POSTGIS_POINTZ_LATITUDE));
        double longitude = Double.parseDouble(matcher.group(PATTERN_POSTGIS_POINTZ_LONGITUDE));
        double altitude = Double.parseDouble(matcher.group(PATTERN_POSTGIS_POINTZ_Z));

        return new GeoCoordinates(
            latitude,
            longitude,
            (int) Math.round(altitude),
            RelationalTrackPoint.POSTGIS_IS_ALTITUDE_UNIT_FEET //
        );
    }

    private void execute(Connection db, String sql) throws SQLException {
        try (Statement stmt = db.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ex) {
            LOGGER.warn("SQL query failed: " + sql, ex);
            throw ex;
        }
    }

    private void execute(Connection db, String sql, ExceptionalConsumer<PreparedStatement, SQLException> parameterSetter) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            parameterSetter.accept(ps);

            ps.execute();
        } catch (SQLException ex) {
            LOGGER.warn("SQL query failed: " + sql, ex);
            throw ex;
        }
    }

    private <EX extends Exception> void benchmark(String name, Class<EX> exceptionClass, ExceptionalRunnable<EX> runnable) throws EX {
        Exception caught = null;

        Instant start = Instant.now();
        try {
            runnable.run();
        } catch (Exception ex) {
            caught = ex;
        }
        Instant end = Instant.now();

        LOGGER.debug("{} took {}ms", name, Duration.between(start, end).toMillis());

        if (caught != null) {
            if (exceptionClass.isInstance(caught)) {
                throw exceptionClass.cast(caught);
            } else {
                throw new RuntimeException("caught unexpected exception", caught);
            }
        }
    }

    private void executeBenchmarked(String name, Connection db, String sql) throws SQLException {
        benchmark(name, SQLException.class, () -> execute(db, sql));
    }

    private void executeBenchmarked(String name, Connection db, String sql, ExceptionalConsumer<PreparedStatement, SQLException> parameterSetter) throws SQLException {
        benchmark(name, SQLException.class, () -> execute(db, sql, parameterSetter));
    }

    private int negativeIfNull(ResultSet rs, String fieldName) throws SQLException {
        int value = rs.getInt(fieldName);

        if (rs.wasNull()) {
            return -1;
        }

        return value;
    }

    private Duration nullableDurationOfMinutes(ResultSet rs, String fieldName) throws SQLException {
        Duration value = Duration.ofMinutes(rs.getInt(fieldName));

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }

    private BarometricPressure nullableBarometricPressureFromCentiInchesOfMercury(ResultSet rs, String fieldName) throws SQLException {
        BarometricPressure value = BarometricPressure.fromInchesOfMercury((double) rs.getInt(fieldName) / 100.0);

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }

    private FlightEvent nullableFlightEvent(ResultSet rs, String fieldName) throws SQLException {
        int id = rs.getInt(fieldName);

        if (rs.wasNull()) {
            return null;
        }

        return caches.getFlightEvents().getEnum(id);
    }

    private PGInterval toPostgresInterval(Duration duration) throws SQLException {
        String s = Long.toString(duration.getSeconds()) + " seconds";
        return new PGInterval(s);
    }
}
