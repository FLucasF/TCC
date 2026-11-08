package com.pousada.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static HikariDataSource dataSource;

    public static void init() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);

        migrate();
    }

    private static void migrate() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INT PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INT NOT NULL,
                        nightly_rate DECIMAL(10, 2) NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id SERIAL PRIMARY KEY,
                        room_number INT NOT NULL,
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(20) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        rate_plan VARCHAR(50) NOT NULL,
                        company_code VARCHAR(50),
                        nights INT NOT NULL,
                        subtotal DECIMAL(10, 2) NOT NULL,
                        discount DECIMAL(10, 2) NOT NULL,
                        total DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                        refund DECIMAL(10, 2),
                        FOREIGN KEY (room_number) REFERENCES rooms(number)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id SERIAL PRIMARY KEY,
                        channel VARCHAR(50) NOT NULL,
                        recipient VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL
                    )
                    """);

            stmt.execute("INSERT INTO rooms VALUES (101, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms VALUES (102, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms VALUES (201, 'DELUXE', 3, 389.90) ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms VALUES (202, 'DELUXE', 3, 389.90) ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms VALUES (301, 'SUITE', 4, 579.90) ON CONFLICT DO NOTHING");
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() throws SQLException {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
