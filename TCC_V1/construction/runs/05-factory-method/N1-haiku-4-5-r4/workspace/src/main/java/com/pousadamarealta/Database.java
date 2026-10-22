package com.pousadamarealta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private final String url;
    private final String user;
    private final String password;

    public Database() {
        this.url = System.getenv("DB_URL");
        this.user = System.getenv("DB_USER");
        this.password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new RuntimeException("Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
        }
    }

    public void initialize() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void createTables(Connection conn) throws SQLException {
        var stmt = conn.createStatement();

        // Messages table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                to_address VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL
            )
        """);

        // Reservations table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14),
                company_cnpj VARCHAR(18),
                company_name VARCHAR(255),
                guest_passport VARCHAR(50),
                guest_country VARCHAR(100),
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10,2) NOT NULL,
                discount NUMERIC(10,2) NOT NULL,
                total NUMERIC(10,2) NOT NULL,
                status VARCHAR(50) NOT NULL,
                amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,
                refund NUMERIC(10,2)
            )
        """);

        stmt.close();
    }
}
