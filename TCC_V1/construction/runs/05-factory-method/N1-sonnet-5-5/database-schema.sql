--
-- PostgreSQL database dump
--

\restrict yBPy0EnSv1AwdDJzZkoIZgMMDe7LS0rg9UmFN2vmzFaA5lBktycNv4tsXUo65jy

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: test_bd4ff42877f24bb886a7fde905cd7614; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA test_bd4ff42877f24bb886a7fde905cd7614;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: messages; Type: TABLE; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

CREATE TABLE test_bd4ff42877f24bb886a7fde905cd7614.messages (
    id bigint NOT NULL,
    channel character varying(20) NOT NULL,
    recipient character varying(200) NOT NULL,
    subject character varying(300) NOT NULL,
    body text NOT NULL
);


--
-- Name: messages_id_seq; Type: SEQUENCE; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

CREATE SEQUENCE test_bd4ff42877f24bb886a7fde905cd7614.messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: messages_id_seq; Type: SEQUENCE OWNED BY; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER SEQUENCE test_bd4ff42877f24bb886a7fde905cd7614.messages_id_seq OWNED BY test_bd4ff42877f24bb886a7fde905cd7614.messages.id;


--
-- Name: reservations; Type: TABLE; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

CREATE TABLE test_bd4ff42877f24bb886a7fde905cd7614.reservations (
    id bigint NOT NULL,
    room_number integer NOT NULL,
    guest_name character varying(200) NOT NULL,
    guest_email character varying(200) NOT NULL,
    identity_kind character varying(20) NOT NULL,
    identity_data text NOT NULL,
    check_in date NOT NULL,
    check_out date NOT NULL,
    guests integer NOT NULL,
    rate_plan character varying(20) NOT NULL,
    nights integer NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    discount numeric(12,2) NOT NULL,
    total numeric(12,2) NOT NULL,
    status character varying(20) NOT NULL,
    amount_paid numeric(12,2) NOT NULL,
    refund numeric(12,2)
);


--
-- Name: reservations_id_seq; Type: SEQUENCE; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

CREATE SEQUENCE test_bd4ff42877f24bb886a7fde905cd7614.reservations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reservations_id_seq; Type: SEQUENCE OWNED BY; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER SEQUENCE test_bd4ff42877f24bb886a7fde905cd7614.reservations_id_seq OWNED BY test_bd4ff42877f24bb886a7fde905cd7614.reservations.id;


--
-- Name: rooms; Type: TABLE; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

CREATE TABLE test_bd4ff42877f24bb886a7fde905cd7614.rooms (
    number integer NOT NULL,
    type character varying(20) NOT NULL,
    capacity integer NOT NULL,
    nightly_rate numeric(12,2) NOT NULL
);


--
-- Name: messages id; Type: DEFAULT; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER TABLE ONLY test_bd4ff42877f24bb886a7fde905cd7614.messages ALTER COLUMN id SET DEFAULT nextval('test_bd4ff42877f24bb886a7fde905cd7614.messages_id_seq'::regclass);


--
-- Name: reservations id; Type: DEFAULT; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER TABLE ONLY test_bd4ff42877f24bb886a7fde905cd7614.reservations ALTER COLUMN id SET DEFAULT nextval('test_bd4ff42877f24bb886a7fde905cd7614.reservations_id_seq'::regclass);


--
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER TABLE ONLY test_bd4ff42877f24bb886a7fde905cd7614.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);


--
-- Name: reservations reservations_pkey; Type: CONSTRAINT; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER TABLE ONLY test_bd4ff42877f24bb886a7fde905cd7614.reservations
    ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);


--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER TABLE ONLY test_bd4ff42877f24bb886a7fde905cd7614.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (number);


--
-- Name: reservations_room_dates; Type: INDEX; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

CREATE INDEX reservations_room_dates ON test_bd4ff42877f24bb886a7fde905cd7614.reservations USING btree (room_number, check_in, check_out);


--
-- Name: reservations reservations_room_number_fkey; Type: FK CONSTRAINT; Schema: test_bd4ff42877f24bb886a7fde905cd7614; Owner: -
--

ALTER TABLE ONLY test_bd4ff42877f24bb886a7fde905cd7614.reservations
    ADD CONSTRAINT reservations_room_number_fkey FOREIGN KEY (room_number) REFERENCES test_bd4ff42877f24bb886a7fde905cd7614.rooms(number);


--
-- PostgreSQL database dump complete
--

\unrestrict yBPy0EnSv1AwdDJzZkoIZgMMDe7LS0rg9UmFN2vmzFaA5lBktycNv4tsXUo65jy

