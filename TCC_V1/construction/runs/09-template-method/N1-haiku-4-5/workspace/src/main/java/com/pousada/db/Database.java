package com.pousada.db;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void initialize() throws SQLException {
        try (Connection conn = getConnection()) {
            createTablesIfNotExist(conn);
        }
    }

    private void createTablesIfNotExist(Connection conn) throws SQLException {
        String[] tables = {
            """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10, 2) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(50) NOT NULL,
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
                FOREIGN KEY(room_number) REFERENCES rooms(number)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL
            )
            """
        };

        for (String sql : tables) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void insertRooms(List<RoomData> rooms) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (RoomData room : rooms) {
                    stmt.setInt(1, room.number);
                    stmt.setString(2, room.type);
                    stmt.setInt(3, room.capacity);
                    stmt.setBigDecimal(4, room.nightlyRate);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }

    public static class RoomData {
        public int number;
        public String type;
        public int capacity;
        public java.math.BigDecimal nightlyRate;

        public RoomData(int number, String type, int capacity, java.math.BigDecimal nightlyRate) {
            this.number = number;
            this.type = type;
            this.capacity = capacity;
            this.nightlyRate = nightlyRate;
        }
    }
}
