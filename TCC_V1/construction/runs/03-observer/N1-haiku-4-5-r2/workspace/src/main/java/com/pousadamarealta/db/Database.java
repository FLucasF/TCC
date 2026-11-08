package com.pousadamarealta.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static HikariDataSource dataSource;

    public static void init(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        dataSource = new HikariDataSource(config);

        initializeSchema();
    }

    private static void initializeSchema() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS rooms (" +
                "number INT PRIMARY KEY," +
                "type VARCHAR(50) NOT NULL," +
                "capacity INT NOT NULL," +
                "nightly_rate NUMERIC(10, 2) NOT NULL" +
                ")");

            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS reservations (" +
                "id BIGSERIAL PRIMARY KEY," +
                "room_number INT NOT NULL," +
                "guest_name VARCHAR(255) NOT NULL," +
                "guest_email VARCHAR(255) NOT NULL," +
                "guest_cpf VARCHAR(14) NOT NULL," +
                "check_in DATE NOT NULL," +
                "check_out DATE NOT NULL," +
                "guests INT NOT NULL," +
                "rate_plan VARCHAR(50) NOT NULL," +
                "nights INT NOT NULL," +
                "subtotal NUMERIC(10, 2) NOT NULL," +
                "discount NUMERIC(10, 2) NOT NULL," +
                "total NUMERIC(10, 2) NOT NULL," +
                "status VARCHAR(50) NOT NULL," +
                "amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0," +
                "refund NUMERIC(10, 2)," +
                "FOREIGN KEY (room_number) REFERENCES rooms(number)" +
                ")");

            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS messages (" +
                "id BIGSERIAL PRIMARY KEY," +
                "channel VARCHAR(50) NOT NULL," +
                "\"to\" VARCHAR(255) NOT NULL," +
                "subject VARCHAR(255) NOT NULL," +
                "body TEXT NOT NULL" +
                ")");

            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_reservations_room_dates " +
                "ON reservations(room_number, check_in, check_out) WHERE status != 'CANCELLED'");

            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_reservations_check_in " +
                "ON reservations(check_in)");

            insertDefaultRooms(conn);
        }
    }

    private static void insertDefaultRooms(Connection conn) throws SQLException {
        var stmt = conn.prepareStatement(
            "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (number) DO NOTHING"
        );

        addRoom(stmt, 101, "STANDARD", 2, "249.90");
        addRoom(stmt, 102, "STANDARD", 2, "249.90");
        addRoom(stmt, 201, "DELUXE", 3, "389.90");
        addRoom(stmt, 202, "DELUXE", 3, "389.90");
        addRoom(stmt, 301, "SUITE", 4, "579.90");

        stmt.executeBatch();
    }

    private static void addRoom(java.sql.PreparedStatement stmt, int number, String type,
                               int capacity, String rate) throws SQLException {
        stmt.setInt(1, number);
        stmt.setString(2, type);
        stmt.setInt(3, capacity);
        stmt.setBigDecimal(4, new java.math.BigDecimal(rate));
        stmt.addBatch();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
