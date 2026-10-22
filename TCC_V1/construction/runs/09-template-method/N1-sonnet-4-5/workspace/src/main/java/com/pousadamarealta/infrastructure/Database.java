package com.pousadamarealta.infrastructure;

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
                    type VARCHAR(20) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10,2) NOT NULL,
                    refund DECIMAL(10,2)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """);

            insertRoomsIfNotExist(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    private void insertRoomsIfNotExist(Connection conn) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM rooms";
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery(checkQuery)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertQuery = "INSERT INTO rooms (number, type) VALUES (?, ?)";
                try (var pstmt = conn.prepareStatement(insertQuery)) {
                    insertRoom(pstmt, "101", "STANDARD");
                    insertRoom(pstmt, "102", "STANDARD");
                    insertRoom(pstmt, "201", "DELUXE");
                    insertRoom(pstmt, "202", "DELUXE");
                    insertRoom(pstmt, "301", "SUITE");
                }
            }
        }
    }

    private void insertRoom(java.sql.PreparedStatement pstmt, String number, String type) throws SQLException {
        pstmt.setString(1, number);
        pstmt.setString(2, type);
        pstmt.executeUpdate();
    }
}
