--
-- PostgreSQL database dump
--

\restrict UsgPsYt2Hcv1ulSqNE7UfPBQvlKw78bMDEAgDh0jTl7zWGOZynoP5nZYgCKiAM1

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
-- Name: message; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.message (
    id bigint NOT NULL,
    channel text NOT NULL,
    recipient text NOT NULL,
    subject text NOT NULL,
    body text NOT NULL
);


--
-- Name: message_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.message_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: message_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.message_id_seq OWNED BY public.message.id;


--
-- Name: reservation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reservation (
    id bigint NOT NULL,
    room_number integer NOT NULL,
    guest_name text NOT NULL,
    guest_email text NOT NULL,
    guest_cpf text NOT NULL,
    check_in date NOT NULL,
    check_out date NOT NULL,
    guests integer NOT NULL,
    rate_plan text NOT NULL,
    nights integer NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    discount numeric(12,2) NOT NULL,
    total numeric(12,2) NOT NULL,
    status text NOT NULL,
    amount_paid numeric(12,2) NOT NULL,
    refund numeric(12,2)
);


--
-- Name: reservation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reservation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reservation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reservation_id_seq OWNED BY public.reservation.id;


--
-- Name: message id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.message ALTER COLUMN id SET DEFAULT nextval('public.message_id_seq'::regclass);


--
-- Name: reservation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservation ALTER COLUMN id SET DEFAULT nextval('public.reservation_id_seq'::regclass);


--
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);


--
-- Name: reservation reservation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_pkey PRIMARY KEY (id);


--
-- Name: reservation_room_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX reservation_room_idx ON public.reservation USING btree (room_number, check_in);


--
-- PostgreSQL database dump complete
--

\unrestrict UsgPsYt2Hcv1ulSqNE7UfPBQvlKw78bMDEAgDh0jTl7zWGOZynoP5nZYgCKiAM1

