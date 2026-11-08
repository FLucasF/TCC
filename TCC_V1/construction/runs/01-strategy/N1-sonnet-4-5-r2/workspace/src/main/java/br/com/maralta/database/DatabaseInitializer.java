package br.com.maralta.database;

import br.com.maralta.domain.RoomType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(Connection connection) {
        try {
            createTables(connection);
            seedRooms(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
            """;

        String createReservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                company_code VARCHAR(50),
                nights INTEGER NOT NULL,
                subtotal DECIMAL(10, 2) NOT NULL,
                discount DECIMAL(10, 2) NOT NULL,
                total DECIMAL(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                refund DECIMAL(10, 2),
                FOREIGN KEY (room_number) REFERENCES rooms(number)
            )
            """;

        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createRoomsTable);
            stmt.execute(createReservationsTable);
            stmt.execute(createMessagesTable);
        }
    }

    private static void seedRooms(Connection connection) throws SQLException {
        String checkRooms = "SELECT COUNT(*) as count FROM rooms";

        try (Statement stmt = connection.createStatement();
             var rs = stmt.executeQuery(checkRooms)) {
            if (rs.next() && rs.getInt("count") > 0) {
                return;
            }
        }

        String insertRooms = """
            INSERT INTO rooms (number, type) VALUES
            ('101', 'STANDARD'),
            ('102', 'STANDARD'),
            ('201', 'DELUXE'),
            ('202', 'DELUXE'),
            ('301', 'SUITE')
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(insertRooms);
        }
    }
}
