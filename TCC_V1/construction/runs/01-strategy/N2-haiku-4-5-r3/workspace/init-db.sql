-- Script de inicialização do banco de dados para a Pousada Maré Alta
-- Execute este script no PostgreSQL antes de rodar a aplicação

CREATE DATABASE pousada;

\c pousada

-- As tabelas serão criadas automaticamente pela aplicação,
-- mas você pode executar este script manualmente se preferir:

CREATE TABLE IF NOT EXISTS rooms (
    number INT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    nightly_rate DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    room_number INT NOT NULL,
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(20) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INT NOT NULL,
    rate_plan VARCHAR(50) NOT NULL,
    company_code VARCHAR(50),
    nights INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    discount DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
    refund DECIMAL(10, 2),
    FOREIGN KEY (room_number) REFERENCES rooms(number)
);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL
);

-- Inserir quartos
INSERT INTO rooms VALUES (101, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING;
INSERT INTO rooms VALUES (102, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING;
INSERT INTO rooms VALUES (201, 'DELUXE', 3, 389.90) ON CONFLICT DO NOTHING;
INSERT INTO rooms VALUES (202, 'DELUXE', 3, 389.90) ON CONFLICT DO NOTHING;
INSERT INTO rooms VALUES (301, 'SUITE', 4, 579.90) ON CONFLICT DO NOTHING;
