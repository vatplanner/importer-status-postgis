package org.vatplanner.importer.postgis.status.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration related to the database.
 */
public class DatabaseConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfiguration.class);

    private String host;
    private int port;
    private String username;
    private String password;
    private String databaseName;
    private String schema;

    public String getHost() {
        return host;
    }

    public DatabaseConfiguration setHost(String host) {
        LOGGER.debug("setting host to {}", host);
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public DatabaseConfiguration setPort(int port) {
        LOGGER.debug("setting port to {}", port);
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DatabaseConfiguration setUsername(String username) {
        LOGGER.debug("setting username to {}", username);
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DatabaseConfiguration setPassword(String password) {
        LOGGER.debug("setting password (details not logged)");
        this.password = password;
        return this;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public DatabaseConfiguration setDatabaseName(String databaseName) {
        LOGGER.debug("setting databaseName to {}", databaseName);
        this.databaseName = databaseName;
        return this;
    }

    public String getSchema() {
        return schema;
    }

    public DatabaseConfiguration setSchema(String schema) {
        LOGGER.debug("setting schema to {}", schema);
        this.schema = schema;
        return this;
    }

}
