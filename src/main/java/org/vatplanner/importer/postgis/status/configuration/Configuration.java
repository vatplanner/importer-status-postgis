package org.vatplanner.importer.postgis.status.configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.client.ClientConfiguration;

/**
 * Loads all configuration for the application.
 */
public class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final DatabaseConfiguration databaseConfig;
    private final ClientConfiguration archiveClientConfig;

    private static final String LOCAL_PROPERTIES_DIRECTORY_NAME = ".vatplanner";
    private static final String LOCAL_PROPERTIES_FILE_NAME = "importer-status-postgis.properties";
    private static final String DEFAULT_PROPERTIES_RESOURCE = LOCAL_PROPERTIES_FILE_NAME;

    /**
     * Loads all application configuration.
     *
     * @param filePath optional path to local configuration file; null to load
     * local configuration from default path
     */
    public Configuration(String filePath) {
        boolean success = true;

        Properties properties = new Properties();
        success &= load(properties, getDefaultConfigurationUrl());
        success &= load(properties, getLocalConfigurationFile(filePath));

        if (!success) {
            throw new RuntimeException("Failed to read configuration");
        }

        databaseConfig = parseDatabaseConfiguration(properties);
        archiveClientConfig = parseArchiveClientConfiguration(properties);
    }

    /**
     * Overloads configuration options from given file into {@link Properties}.
     *
     * @param properties properties to receive overloaded options
     * @param file configuration file to read
     * @return true if file was read successfully, false if an error occurred
     */
    private boolean load(Properties properties, File file) {
        LOGGER.info("Loading configuration from file {}", file);

        try (FileReader fr = new FileReader(file)) {
            properties.load(fr);
            return true;
        } catch (IOException ex) {
            LOGGER.error("Failed to read configuration from file " + file, ex);
            return false;
        }
    }

    /**
     * Overloads configuration options from given {@link URL} into
     * {@link Properties}.
     *
     * @param properties properties to receive overloaded options
     * @param url URL of configuration file to read
     * @return true if file was read successfully, false if an error occurred
     */
    private boolean load(Properties properties, URL url) {
        LOGGER.info("Loading configuration from URL {}", url);
        try (InputStream is = url.openStream()) {
            properties.load(is);
            return true;
        } catch (IOException ex) {
            LOGGER.error("Failed to read configuration from URL " + url, ex);
            return false;
        }
    }

    /**
     * Returns a {@link File} reference to the default configuration file. The
     * default configuration is shipped with the application and should always
     * be loaded first to provide a base configuration, so user only has to
     * configure overrides.
     *
     * @return reference to the default configuration file
     */
    private URL getDefaultConfigurationUrl() {
        return getClass().getClassLoader().getResource(DEFAULT_PROPERTIES_RESOURCE);
    }

    /**
     * Returns a {@link File} reference to the local configuration file. The
     * local configuration is by default sought at a well-known location. If
     * set, default location can be overridden.
     *
     * @param filePath optional override for a specific file; default location
     * if null
     * @return reference to the local configuration file
     */
    private File getLocalConfigurationFile(String filePath) {
        if (filePath == null) {
            filePath = System.getProperty("user.home") + File.separator + LOCAL_PROPERTIES_DIRECTORY_NAME + File.separator + LOCAL_PROPERTIES_FILE_NAME;
        }

        return new File(filePath);
    }

    public ClientConfiguration getArchiveClientConfig() {
        return archiveClientConfig;
    }

    public DatabaseConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    private DatabaseConfiguration parseDatabaseConfiguration(Properties properties) {
        DatabaseConfiguration config = new DatabaseConfiguration();

        setString(properties, "database.host", config::setHost);
        setInteger(properties, "database.port", config::setPort);
        setString(properties, "database.username", config::setUsername);
        setString(properties, "database.password", config::setPassword);
        setString(properties, "database.dbname", config::setDatabaseName);
        setString(properties, "database.schema", config::setSchema);

        return config;
    }

    private ClientConfiguration parseArchiveClientConfiguration(Properties properties) {
        ClientConfiguration config = new ClientConfiguration();

        setString(properties, "archive.amqp.host", config::setAmqpHost);
        setInteger(properties, "archive.amqp.port", config::setAmqpPort);
        setString(properties, "archive.amqp.username", config::setAmqpUsername);
        setString(properties, "archive.amqp.password", config::setAmqpPassword);
        setString(properties, "archive.amqp.virtualHost", config::setAmqpVirtualHost);

        setString(properties, "archive.requests.exchange", config::setRequestsExchange);

        return config;
    }

    private void setString(Properties properties, String propertiesKey, Consumer<String> consumer) {
        consumer.accept(properties.getProperty(propertiesKey));
    }

    private void setInteger(Properties properties, String propertiesKey, Consumer<Integer> consumer) {
        consumer.accept(Integer.parseInt(properties.getProperty(propertiesKey)));
    }
}
