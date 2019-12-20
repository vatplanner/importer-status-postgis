package org.vatplanner.importer.postgis.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides methods to save to and load from a PostGIS database.
 */
public class Database {
    // TODO: use connection pool

    private final String url;
    private final Properties properties;

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

}
