package com.pousadamareal.config;

import java.sql.*;

public class DatabaseConfig {
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void init() {
        try {
            Class.forName("org.postgresql.Driver");
            executeSqlScript();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    private static void executeSqlScript() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String schema = """
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(50) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate NUMERIC(10, 2) NOT NULL
                );

                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20),
                    company_cnpj VARCHAR(20),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10, 2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                );

                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient_email VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );

                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                    (101, 'STANDARD', 2, 249.90),
                    (102, 'STANDARD', 2, 249.90),
                    (201, 'DELUXE', 3, 389.90),
                    (202, 'DELUXE', 3, 389.90),
                    (301, 'SUITE', 4, 579.90)
                ON CONFLICT (number) DO NOTHING;
                """;

            for (String statement : schema.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim() + ";");
                }
            }
        }
    }
}
