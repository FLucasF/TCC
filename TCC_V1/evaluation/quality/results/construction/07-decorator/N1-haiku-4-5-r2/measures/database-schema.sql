--
-- PostgreSQL database dump
--

\restrict 6naiLk4OKKOn644ziGKMTf4oOFFm9hmm0fMuybPafymWs9cBMId1SN1jC7V85ed

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
    id bigint NOT NULL,
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
    amount_paid numeric(10,2) DEFAULT 0 NOT NULL,
    refund numeric(10,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: reservations reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reservations
    ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

\unrestrict 6naiLk4OKKOn644ziGKMTf4oOFFm9hmm0fMuybPafymWs9cBMId1SN1jC7V85ed

