--
-- PostgreSQL database dump
--

\restrict AXhU8ha4AsvYt5eTMrZVfXaHbkk5R4JFO6we5Cghk2Ie5YPceKNoKTQRrOYfV2B

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.messages (
    id integer NOT NULL,
    channel character varying(20) NOT NULL,
    recipient character varying(255) NOT NULL,
    subject text NOT NULL,
    body text NOT NULL,
    status character varying(20) NOT NULL
);


--
-- Name: messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.messages_id_seq OWNED BY public.messages.id;


--
-- Name: reservations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reservations (
    id integer NOT NULL,
    room_number character varying(10) NOT NULL,
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
    amount_paid numeric(10,2) DEFAULT 0 NOT NULL,
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
-- Name: messages id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.messages ALTER COLUMN id SET DEFAULT nextval('public.messages_id_seq'::regclass);


--
-- Name: reservations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservations ALTER COLUMN id SET DEFAULT nextval('public.reservations_id_seq'::regclass);


--
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);


--
-- Name: reservations reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservations
    ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

\unrestrict AXhU8ha4AsvYt5eTMrZVfXaHbkk5R4JFO6we5Cghk2Ie5YPceKNoKTQRrOYfV2B

