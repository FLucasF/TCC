package com.posadamareaita.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public class Database {
    private static HikariDataSource dataSource;

    public static void initialize(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        createTables();
    }

    private static void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Rooms table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INT PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INT NOT NULL,
                        nightly_rate NUMERIC(10, 2) NOT NULL
                    )
                    """);

            // Reservations table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INT NOT NULL REFERENCES rooms(number),
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        rate_plan VARCHAR(20) NOT NULL,
                        nights INT NOT NULL,
                        subtotal NUMERIC(10, 2) NOT NULL,
                        discount NUMERIC(10, 2) NOT NULL,
                        total NUMERIC(10, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                        refund NUMERIC(10, 2),
                        guest_cpf VARCHAR(20),
                        company_cnpj VARCHAR(20),
                        company_name VARCHAR(255),
                        guest_passport VARCHAR(20),
                        guest_country VARCHAR(255)
                    )
                    """);

            // Messages table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(20) NOT NULL,
                        recipient VARCHAR(255) NOT NULL,
                        subject VARCHAR(255),
                        body TEXT NOT NULL
                    )
                    """);

            // Insert rooms if they don't exist
            insertDefaultRooms();
        }
    }

    private static void insertDefaultRooms() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if rooms already exist
            var resultSet = stmt.executeQuery("SELECT COUNT(*) as count FROM rooms");
            resultSet.next();
            if (resultSet.getInt("count") > 0) {
                return;
            }

            stmt.execute("INSERT INTO rooms VALUES (101, 'STANDARD', 2, 249.90)");
            stmt.execute("INSERT INTO rooms VALUES (102, 'STANDARD', 2, 249.90)");
            stmt.execute("INSERT INTO rooms VALUES (201, 'DELUXE', 3, 389.90)");
            stmt.execute("INSERT INTO rooms VALUES (202, 'DELUXE', 3, 389.90)");
            stmt.execute("INSERT INTO rooms VALUES (301, 'SUITE', 4, 579.90)");
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
