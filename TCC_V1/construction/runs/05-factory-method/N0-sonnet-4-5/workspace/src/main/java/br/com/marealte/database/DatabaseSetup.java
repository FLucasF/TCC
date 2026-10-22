package br.com.marealte.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {
    public static void initialize() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate DECIMAL(10, 2) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
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
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject TEXT NOT NULL,
                    body TEXT NOT NULL
                )
            """);

            insertRoomsIfEmpty(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void insertRoomsIfEmpty(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (var insertStmt = conn.prepareStatement(
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)")) {

                    insertRoom(insertStmt, "101", "STANDARD", 2, 249.90);
                    insertRoom(insertStmt, "102", "STANDARD", 2, 249.90);
                    insertRoom(insertStmt, "201", "DELUXE", 3, 389.90);
                    insertRoom(insertStmt, "202", "DELUXE", 3, 389.90);
                    insertRoom(insertStmt, "301", "SUITE", 4, 579.90);
                }
            }
        }
    }

    private static void insertRoom(java.sql.PreparedStatement stmt, String number, String type, int capacity, double rate) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type);
        stmt.setInt(3, capacity);
        stmt.setBigDecimal(4, java.math.BigDecimal.valueOf(rate));
        stmt.executeUpdate();
    }
}
