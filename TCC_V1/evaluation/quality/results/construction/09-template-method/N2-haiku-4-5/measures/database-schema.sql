--
-- PostgreSQL database dump
--

\restrict QpKmIwEoxL2JWIenQrHj9OgRJBpCRlq3Ajq4zC0ivOgqsKwuDvLfRX8Nnr9OliC

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6 (Ubuntu 18.6-1.pgdg24.04+2)

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: reservations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reservations (
    id integer NOT NULL,
    room_number integer NOT NULL,
    guest_name character varying(255) NOT NULL,
    guest_email character varying(255) NOT NULL,
    guest_cpf character varying(14) NOT NULL,
    check_in date NOT NULL,
    check_out date NOT NULL,
    guests integer NOT NULL,
    rate_plan character varying(20) NOT NULL,
    nights integer NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    discount numeric(10,2) NOT NULL,
    total numeric(10,2) NOT NULL,
    status character varying(20) NOT NULL,
    amount_paid numeric(10,2) DEFAULT 0,
    refund numeric(10,2)
);


--
-- Name: reservations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reservations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reservations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reservations_id_seq OWNED BY public.reservations.id;


--
-- Name: rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rooms (
    number integer NOT NULL,
    type character varying(20) NOT NULL,
    capacity integer NOT NULL,
    nightly_rate numeric(10,2) NOT NULL
);


--
-- Name: sent_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sent_messages (
    id integer NOT NULL,
    channel character varying(20) NOT NULL,
    to_address character varying(255) NOT NULL,
    subject character varying(255) NOT NULL,
    body text NOT NULL
);


--
-- Name: sent_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sent_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sent_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sent_messages_id_seq OWNED BY public.sent_messages.id;


--
-- Name: reservations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservations ALTER COLUMN id SET DEFAULT nextval('public.reservations_id_seq'::regclass);


--
-- Name: sent_messages id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sent_messages ALTER COLUMN id SET DEFAULT nextval('public.sent_messages_id_seq'::regclass);


--
-- Name: reservations reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservations
    ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);


--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (number);


--
-- Name: sent_messages sent_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sent_messages
    ADD CONSTRAINT sent_messages_pkey PRIMARY KEY (id);


--
-- Name: idx_reservations_check_in; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reservations_check_in ON public.reservations USING btree (check_in);


--
-- Name: idx_reservations_room_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reservations_room_dates ON public.reservations USING btree (room_number, check_in, check_out, status);


--
-- Name: idx_reservations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reservations_status ON public.reservations USING btree (status);


--
-- Name: reservations reservations_room_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservations
    ADD CONSTRAINT reservations_room_number_fkey FOREIGN KEY (room_number) REFERENCES public.rooms(number);


--
-- PostgreSQL database dump complete
--

\unrestrict QpKmIwEoxL2JWIenQrHj9OgRJBpCRlq3Ajq4zC0ivOgqsKwuDvLfRX8Nnr9OliC

