package com.pousadamareal.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    public static void initialize() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            insertInitialData(conn);
        } catch (SQLException e) {
            logger.error("Erro ao inicializar banco de dados", e);
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(20) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10, 2) NOT NULL
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(11),
                company_cnpj VARCHAR(14),
                company_name VARCHAR(255),
                guest_passport VARCHAR(20),
                guest_country VARCHAR(2),
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10, 2) NOT NULL,
                discount NUMERIC(10, 2) NOT NULL,
                total NUMERIC(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                refund NUMERIC(10, 2)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(255),
                body TEXT NOT NULL
            )
        """);

        stmt.close();
    }

    private static void insertInitialData(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        String checkRooms = "SELECT COUNT(*) FROM rooms";
        ResultSet rs = stmt.executeQuery(checkRooms);
        rs.next();
        if (rs.getInt(1) == 0) {
            stmt.execute("INSERT INTO rooms VALUES (101, 'STANDARD', 2, 249.90)");
            stmt.execute("INSERT INTO rooms VALUES (102, 'STANDARD', 2, 249.90)");
            stmt.execute("INSERT INTO rooms VALUES (201, 'DELUXE', 3, 389.90)");
            stmt.execute("INSERT INTO rooms VALUES (202, 'DELUXE', 3, 389.90)");
            stmt.execute("INSERT INTO rooms VALUES (301, 'SUITE', 4, 579.90)");
        }

        stmt.close();
    }
}
