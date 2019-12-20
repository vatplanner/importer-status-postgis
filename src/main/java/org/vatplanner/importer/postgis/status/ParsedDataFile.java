package org.vatplanner.importer.postgis.status;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.common.RawDataFile;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFile;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFileParser;

/**
 * Holds a parsed {@link DataFile} and additional meta information in the same
 * way as {@link RawDataFile}.
 */
public class ParsedDataFile {
    // TODO: check if {@link RawDataFile} can be split into an abstract base class which can be reused
    // TODO: move to data formats module?

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedDataFile.class);
    private static final DataFileParser PARSER = new DataFileParser();

    private String fetchNode;
    private Instant fetchTime;
    private DataFile content;
    private String fetchUrlRequested;
    private String fetchUrlRetrieved;

    /**
     * Parses the given {@link RawDataFile} maintaining all meta information.
     * After parsing, original content will be cleared.
     *
     * @param rawDataFile data file to be parsed
     */
    public ParsedDataFile(RawDataFile rawDataFile) {
        ByteArrayInputStream bais = new ByteArrayInputStream(rawDataFile.getData());
        BufferedReader br = new BufferedReader(new InputStreamReader(bais, StandardCharsets.ISO_8859_1));
        content = PARSER.parse(br);
        try {
            bais.close();
        } catch (IOException ex) {
            LOGGER.warn("Failed to close byte array stream; this may result in a memory leak!", ex);
        }

        fetchNode = rawDataFile.getFetchNode();
        fetchTime = rawDataFile.getFetchTime();
        fetchUrlRequested = rawDataFile.getFetchUrlRequested();
        fetchUrlRetrieved = rawDataFile.getFetchUrlRetrieved();

        rawDataFile.clear();
    }

    /**
     * Returns the identification of the node who has fetched the data.
     *
     * @return identification of the node who has fetched the data
     */
    public String getFetchNode() {
        return fetchNode;
    }

    /**
     * Returns the timestamp of when fetched data had been requested.
     *
     * @return timestamp of when fetched data had been requested
     */
    public Instant getFetchTime() {
        return fetchTime;
    }

    /**
     * Returns the parsed {@link DataFile}.
     *
     * @return parsed data file
     */
    public DataFile getContent() {
        return content;
    }

    /**
     * Returns the original URL requested to retrieve fetched data from (before
     * following redirects).
     *
     * @return original URL requested to retrieve fetched data from
     */
    public String getFetchUrlRequested() {
        return fetchUrlRequested;
    }

    /**
     * Returns the actual URL where fetched data had been retrieved from (after
     * following redirects).
     *
     * @return actual URL where fetched data had been retrieved from
     */
    public String getFetchUrlRetrieved() {
        return fetchUrlRetrieved;
    }

}
