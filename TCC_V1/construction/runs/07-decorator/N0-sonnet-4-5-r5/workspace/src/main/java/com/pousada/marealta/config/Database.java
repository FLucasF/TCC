package com.pousada.marealta.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabela de quartos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate DECIMAL(10,2) NOT NULL
                )
            """);

            // Tabela de reservas
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(200) NOT NULL,
                    guest_email VARCHAR(200) NOT NULL,
                    guest_cpf VARCHAR(20) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10,2) NOT NULL,
                    discount DECIMAL(10,2) NOT NULL,
                    total DECIMAL(10,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10,2)
                )
            """);

            // Tabela de mensagens
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(200) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
            """);

            // Insere os quartos fixos se não existirem
            stmt.execute("""
                INSERT INTO rooms (number, type, capacity, nightly_rate)
                VALUES
                    ('101', 'STANDARD', 2, 249.90),
                    ('102', 'STANDARD', 2, 249.90),
                    ('201', 'DELUXE', 3, 389.90),
                    ('202', 'DELUXE', 3, 389.90),
                    ('301', 'SUITE', 4, 579.90)
                ON CONFLICT (number) DO NOTHING
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
