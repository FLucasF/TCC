package com.pousadamarealta.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private DatabaseManager(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        initializeDatabase();
    }

    public static void initialize(String dbUrl, String dbUser, String dbPassword) {
        instance = new DatabaseManager(dbUrl, dbUser, dbPassword);
    }

    public static DatabaseManager getInstance() {
        return instance;
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createTables(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "number INT PRIMARY KEY," +
                    "type VARCHAR(50) NOT NULL," +
                    "capacity INT NOT NULL," +
                    "nightly_rate DECIMAL(10,2) NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "room_number INT NOT NULL," +
                    "guest_name VARCHAR(255) NOT NULL," +
                    "guest_email VARCHAR(255) NOT NULL," +
                    "guest_cpf VARCHAR(11) NOT NULL," +
                    "check_in DATE NOT NULL," +
                    "check_out DATE NOT NULL," +
                    "guests INT NOT NULL," +
                    "rate_plan VARCHAR(50) NOT NULL," +
                    "status VARCHAR(50) NOT NULL," +
                    "nights INT NOT NULL," +
                    "subtotal DECIMAL(10,2) NOT NULL," +
                    "discount DECIMAL(10,2) NOT NULL," +
                    "total DECIMAL(10,2) NOT NULL," +
                    "amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0," +
                    "refund DECIMAL(10,2)," +
                    "FOREIGN KEY (room_number) REFERENCES rooms(number)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "channel VARCHAR(50) NOT NULL," +
                    "to_address VARCHAR(255) NOT NULL," +
                    "subject VARCHAR(255) NOT NULL," +
                    "body TEXT NOT NULL" +
                    ")");
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
