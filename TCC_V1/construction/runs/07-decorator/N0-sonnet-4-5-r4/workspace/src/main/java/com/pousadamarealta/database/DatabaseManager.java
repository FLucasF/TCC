package com.pousadamarealta.database;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final DataSource dataSource;

    public DatabaseManager(String url, String user, String password) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(url);
        ds.setUser(user);
        ds.setPassword(password);
        this.dataSource = ds;
        initializeDatabase();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void initializeDatabase() {
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL,
                capacity INT NOT NULL,
                nightly_rate DECIMAL(10, 2) NOT NULL
            )
            """;

        String createReservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
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
                refund DECIMAL(10, 2),
                FOREIGN KEY (room_number) REFERENCES rooms(number)
            )
            """;

        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(20) NOT NULL
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createRoomsTable);
            stmt.execute(createReservationsTable);
            stmt.execute(createMessagesTable);
            initializeRooms();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void initializeRooms() {
        String checkRooms = "SELECT COUNT(*) FROM rooms";
        String insertRoom = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(checkRooms)) {

            if (rs.next() && rs.getInt(1) == 0) {
                try (var pstmt = conn.prepareStatement(insertRoom)) {
                    pstmt.setString(1, "101");
                    pstmt.setString(2, "STANDARD");
                    pstmt.setInt(3, 2);
                    pstmt.setBigDecimal(4, new java.math.BigDecimal("249.90"));
                    pstmt.executeUpdate();

                    pstmt.setString(1, "102");
                    pstmt.setString(2, "STANDARD");
                    pstmt.setInt(3, 2);
                    pstmt.setBigDecimal(4, new java.math.BigDecimal("249.90"));
                    pstmt.executeUpdate();

                    pstmt.setString(1, "201");
                    pstmt.setString(2, "DELUXE");
                    pstmt.setInt(3, 3);
                    pstmt.setBigDecimal(4, new java.math.BigDecimal("389.90"));
                    pstmt.executeUpdate();

                    pstmt.setString(1, "202");
                    pstmt.setString(2, "DELUXE");
                    pstmt.setInt(3, 3);
                    pstmt.setBigDecimal(4, new java.math.BigDecimal("389.90"));
                    pstmt.executeUpdate();

                    pstmt.setString(1, "301");
                    pstmt.setString(2, "SUITE");
                    pstmt.setInt(3, 4);
                    pstmt.setBigDecimal(4, new java.math.BigDecimal("579.90"));
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }
}
