package com.pousadamarealta.database;

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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INT NOT NULL,
                    nightly_rate DECIMAL(10, 2) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(11) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INT NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL,
                    refund DECIMAL(10, 2)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """);

            insertRoomsIfEmpty(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inicializar banco de dados", e);
        }
    }

    private void insertRoomsIfEmpty(Connection conn) throws SQLException {
        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM rooms");
        rs.next();
        if (rs.getInt(1) == 0) {
            try (var stmt = conn.prepareStatement(
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)")) {

                insertRoom(stmt, "101", "STANDARD", 2, "249.90");
                insertRoom(stmt, "102", "STANDARD", 2, "249.90");
                insertRoom(stmt, "201", "DELUXE", 3, "389.90");
                insertRoom(stmt, "202", "DELUXE", 3, "389.90");
                insertRoom(stmt, "301", "SUITE", 4, "579.90");
            }
        }
    }

    private void insertRoom(java.sql.PreparedStatement stmt, String number, String type,
                           int capacity, String rate) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type);
        stmt.setInt(3, capacity);
        stmt.setBigDecimal(4, new java.math.BigDecimal(rate));
        stmt.executeUpdate();
    }
}
