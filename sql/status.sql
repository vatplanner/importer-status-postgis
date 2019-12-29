--
-- PostgreSQL database dump
--

-- Dumped from database version 12.1
-- Dumped by pg_dump version 12.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: status; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA status;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: connections; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.connections (
    logontime timestamp with time zone NOT NULL,
    vatsimid integer NOT NULL,
    firstreport_id integer NOT NULL,
    lastreport_id integer NOT NULL,
    connection_id integer NOT NULL
);


--
-- Name: connections_connection_id_seq; Type: SEQUENCE; Schema: status; Owner: -
--

CREATE SEQUENCE status.connections_connection_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: connections_connection_id_seq; Type: SEQUENCE OWNED BY; Schema: status; Owner: -
--

ALTER SEQUENCE status.connections_connection_id_seq OWNED BY status.connections.connection_id;


--
-- Name: connections_flights; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.connections_flights (
    flight_id integer NOT NULL,
    connection_id integer NOT NULL
);


--
-- Name: facilities; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.facilities (
    name character varying(20) NOT NULL,
    connection_id integer NOT NULL
);


--
-- Name: fetchnodes; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.fetchnodes (
    fetchnode_id smallint NOT NULL,
    name character varying(16) NOT NULL
);


--
-- Name: fetchnodes_fetchnode_id_seq; Type: SEQUENCE; Schema: status; Owner: -
--

CREATE SEQUENCE status.fetchnodes_fetchnode_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fetchnodes_fetchnode_id_seq; Type: SEQUENCE OWNED BY; Schema: status; Owner: -
--

ALTER SEQUENCE status.fetchnodes_fetchnode_id_seq OWNED BY status.fetchnodes.fetchnode_id;


--
-- Name: fetchurls; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.fetchurls (
    fetchurl_id integer NOT NULL,
    url text NOT NULL
);


--
-- Name: fetchurls_fetchurl_id_seq; Type: SEQUENCE; Schema: status; Owner: -
--

CREATE SEQUENCE status.fetchurls_fetchurl_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fetchurls_fetchurl_id_seq; Type: SEQUENCE OWNED BY; Schema: status; Owner: -
--

ALTER SEQUENCE status.fetchurls_fetchurl_id_seq OWNED BY status.fetchurls.fetchurl_id;


--
-- Name: flightplans; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.flightplans (
    flight_id integer NOT NULL,
    revision smallint NOT NULL,
    firstseen_report_id integer NOT NULL,
    flightplantype character(1),
    route text NOT NULL,
    altitudefeet integer,
    minutesenroute smallint,
    minutesfuel smallint,
    departureairport character varying(50) NOT NULL,
    destinationairport character varying(50) NOT NULL,
    alternateairport character varying(50),
    aircrafttype character varying(50),
    departuretimeplanned timestamp with time zone
);


--
-- Name: flights; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.flights (
    flight_id integer NOT NULL,
    vatsimid integer NOT NULL,
    callsign character varying(50) NOT NULL
);


--
-- Name: flights_flight_id_seq; Type: SEQUENCE; Schema: status; Owner: -
--

CREATE SEQUENCE status.flights_flight_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: flights_flight_id_seq; Type: SEQUENCE OWNED BY; Schema: status; Owner: -
--

ALTER SEQUENCE status.flights_flight_id_seq OWNED BY status.flights.flight_id;


--
-- Name: reports; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.reports (
    report_id integer NOT NULL,
    recordtime timestamp with time zone NOT NULL,
    connectedclients integer NOT NULL,
    fetchtime timestamp with time zone NOT NULL,
    fetchurlrequested_id integer NOT NULL,
    fetchnode_id smallint,
    parsetime timestamp with time zone NOT NULL,
    parserrejectedlines integer NOT NULL,
    fetchurlretrieved_id integer
);


--
-- Name: reports_report_id_seq; Type: SEQUENCE; Schema: status; Owner: -
--

CREATE SEQUENCE status.reports_report_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reports_report_id_seq; Type: SEQUENCE OWNED BY; Schema: status; Owner: -
--

ALTER SEQUENCE status.reports_report_id_seq OWNED BY status.reports.report_id;


--
-- Name: trackpoints; Type: TABLE; Schema: status; Owner: -
--

CREATE TABLE status.trackpoints (
    report_id integer NOT NULL,
    flight_id integer NOT NULL,
    geocoords public.geography(PointZ,4326) NOT NULL,
    heading smallint,
    groundspeed smallint,
    transpondercode smallint,
    qnhcinhg smallint,
    qnhhpa smallint
);


--
-- Name: connections connection_id; Type: DEFAULT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections ALTER COLUMN connection_id SET DEFAULT nextval('status.connections_connection_id_seq'::regclass);


--
-- Name: fetchnodes fetchnode_id; Type: DEFAULT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.fetchnodes ALTER COLUMN fetchnode_id SET DEFAULT nextval('status.fetchnodes_fetchnode_id_seq'::regclass);


--
-- Name: fetchurls fetchurl_id; Type: DEFAULT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.fetchurls ALTER COLUMN fetchurl_id SET DEFAULT nextval('status.fetchurls_fetchurl_id_seq'::regclass);


--
-- Name: flights flight_id; Type: DEFAULT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.flights ALTER COLUMN flight_id SET DEFAULT nextval('status.flights_flight_id_seq'::regclass);


--
-- Name: reports report_id; Type: DEFAULT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.reports ALTER COLUMN report_id SET DEFAULT nextval('status.reports_report_id_seq'::regclass);


--
-- Name: connections_flights connections_flights_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections_flights
    ADD CONSTRAINT connections_flights_pkey PRIMARY KEY (flight_id, connection_id);


--
-- Name: connections connections_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections
    ADD CONSTRAINT connections_pkey PRIMARY KEY (connection_id);


--
-- Name: facilities facilities_unique_connection; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.facilities
    ADD CONSTRAINT facilities_unique_connection UNIQUE (connection_id);


--
-- Name: fetchnodes fetchnodes_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.fetchnodes
    ADD CONSTRAINT fetchnodes_pkey PRIMARY KEY (fetchnode_id);


--
-- Name: fetchnodes fetchnodes_unique_name; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.fetchnodes
    ADD CONSTRAINT fetchnodes_unique_name UNIQUE (name);


--
-- Name: fetchurls fetchurls_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.fetchurls
    ADD CONSTRAINT fetchurls_pkey PRIMARY KEY (fetchurl_id);


--
-- Name: fetchurls fetchurls_unique_url; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.fetchurls
    ADD CONSTRAINT fetchurls_unique_url UNIQUE (url);


--
-- Name: flightplans flightplans_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.flightplans
    ADD CONSTRAINT flightplans_pkey PRIMARY KEY (flight_id, revision);


--
-- Name: flightplans flightplans_unique_flight_revision; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.flightplans
    ADD CONSTRAINT flightplans_unique_flight_revision UNIQUE (flight_id, revision);


--
-- Name: flights flights_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.flights
    ADD CONSTRAINT flights_pkey PRIMARY KEY (flight_id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (report_id);


--
-- Name: reports reports_unique_recordtime; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.reports
    ADD CONSTRAINT reports_unique_recordtime UNIQUE (recordtime);


--
-- Name: trackpoints trackpoints_pkey; Type: CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.trackpoints
    ADD CONSTRAINT trackpoints_pkey PRIMARY KEY (report_id, flight_id);


--
-- Name: fki_connections_fk_reports_first; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_connections_fk_reports_first ON status.connections USING btree (firstreport_id);


--
-- Name: fki_connections_fk_reports_last; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_connections_fk_reports_last ON status.connections USING btree (lastreport_id);


--
-- Name: fki_connections_flights_fk_connections; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_connections_flights_fk_connections ON status.connections_flights USING btree (connection_id);


--
-- Name: fki_connections_flights_fk_flights; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_connections_flights_fk_flights ON status.connections_flights USING btree (flight_id);


--
-- Name: fki_facilities_fk_connections; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_facilities_fk_connections ON status.facilities USING btree (connection_id);


--
-- Name: fki_flightplans_fk_flights; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_flightplans_fk_flights ON status.flightplans USING btree (flight_id);


--
-- Name: fki_flightplans_fk_reports_firstseen; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_flightplans_fk_reports_firstseen ON status.flightplans USING btree (firstseen_report_id);


--
-- Name: fki_reports_fk_fetchnodes; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_reports_fk_fetchnodes ON status.reports USING btree (fetchnode_id);


--
-- Name: fki_reports_fk_fetchurls_requested; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_reports_fk_fetchurls_requested ON status.reports USING btree (fetchurlrequested_id);


--
-- Name: fki_reports_fk_fetchurls_retrieved; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_reports_fk_fetchurls_retrieved ON status.reports USING btree (fetchurlretrieved_id);


--
-- Name: fki_trackpoints_fk_flights; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_trackpoints_fk_flights ON status.trackpoints USING btree (flight_id);


--
-- Name: fki_trackpoints_fk_reports; Type: INDEX; Schema: status; Owner: -
--

CREATE INDEX fki_trackpoints_fk_reports ON status.trackpoints USING btree (report_id);


--
-- Name: connections connections_fk_reports_first; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections
    ADD CONSTRAINT connections_fk_reports_first FOREIGN KEY (firstreport_id) REFERENCES status.reports(report_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: connections connections_fk_reports_last; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections
    ADD CONSTRAINT connections_fk_reports_last FOREIGN KEY (lastreport_id) REFERENCES status.reports(report_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: connections_flights connections_flights_fk_connections; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections_flights
    ADD CONSTRAINT connections_flights_fk_connections FOREIGN KEY (connection_id) REFERENCES status.connections(connection_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: connections_flights connections_flights_fk_flights; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.connections_flights
    ADD CONSTRAINT connections_flights_fk_flights FOREIGN KEY (flight_id) REFERENCES status.flights(flight_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: facilities facilities_fk_connections; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.facilities
    ADD CONSTRAINT facilities_fk_connections FOREIGN KEY (connection_id) REFERENCES status.connections(connection_id) ON UPDATE CASCADE ON DELETE RESTRICT NOT VALID;


--
-- Name: flightplans flightplans_fk_flights; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.flightplans
    ADD CONSTRAINT flightplans_fk_flights FOREIGN KEY (flight_id) REFERENCES status.flights(flight_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: flightplans flightplans_fk_reports_firstseen; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.flightplans
    ADD CONSTRAINT flightplans_fk_reports_firstseen FOREIGN KEY (firstseen_report_id) REFERENCES status.reports(report_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: reports reports_fk_fetchnodes; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.reports
    ADD CONSTRAINT reports_fk_fetchnodes FOREIGN KEY (fetchnode_id) REFERENCES status.fetchnodes(fetchnode_id) ON UPDATE CASCADE ON DELETE RESTRICT NOT VALID;


--
-- Name: reports reports_fk_fetchurls_requested; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.reports
    ADD CONSTRAINT reports_fk_fetchurls_requested FOREIGN KEY (fetchurlrequested_id) REFERENCES status.fetchurls(fetchurl_id) ON UPDATE CASCADE ON DELETE RESTRICT NOT VALID;


--
-- Name: reports reports_fk_fetchurls_retrieved; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.reports
    ADD CONSTRAINT reports_fk_fetchurls_retrieved FOREIGN KEY (fetchurlretrieved_id) REFERENCES status.fetchurls(fetchurl_id) ON UPDATE CASCADE ON DELETE RESTRICT NOT VALID;


--
-- Name: trackpoints trackpoints_fk_flights; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.trackpoints
    ADD CONSTRAINT trackpoints_fk_flights FOREIGN KEY (flight_id) REFERENCES status.flights(flight_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- Name: trackpoints trackpoints_fk_reports; Type: FK CONSTRAINT; Schema: status; Owner: -
--

ALTER TABLE ONLY status.trackpoints
    ADD CONSTRAINT trackpoints_fk_reports FOREIGN KEY (report_id) REFERENCES status.reports(report_id) ON UPDATE CASCADE ON DELETE CASCADE NOT VALID;


--
-- PostgreSQL database dump complete
--

