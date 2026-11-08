package com.pousada.marealta.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static HikariDataSource dataSource;

    public static void init() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("DB_URL, DB_USER, and DB_PASSWORD environment variables must be set");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);

        createTables();
        logger.info("Database initialized successfully");
    }

    private static void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String[] createTableStatements = {
                "CREATE TABLE IF NOT EXISTS rooms (" +
                "    number INT PRIMARY KEY," +
                "    type VARCHAR(20) NOT NULL," +
                "    capacity INT NOT NULL," +
                "    nightly_rate NUMERIC(10, 2) NOT NULL" +
                ")",

                "CREATE TABLE IF NOT EXISTS reservations (" +
                "    id SERIAL PRIMARY KEY," +
                "    room_number INT NOT NULL REFERENCES rooms(number)," +
                "    guest_name VARCHAR(255) NOT NULL," +
                "    guest_email VARCHAR(255) NOT NULL," +
                "    guest_cpf VARCHAR(20) NOT NULL," +
                "    check_in DATE NOT NULL," +
                "    check_out DATE NOT NULL," +
                "    guests INT NOT NULL," +
                "    rate_plan VARCHAR(50) NOT NULL," +
                "    company_code VARCHAR(50)," +
                "    nights INT NOT NULL," +
                "    subtotal NUMERIC(10, 2) NOT NULL," +
                "    discount NUMERIC(10, 2) NOT NULL," +
                "    total NUMERIC(10, 2) NOT NULL," +
                "    status VARCHAR(50) NOT NULL," +
                "    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0," +
                "    refund NUMERIC(10, 2)" +
                ")",

                "CREATE TABLE IF NOT EXISTS messages (" +
                "    id SERIAL PRIMARY KEY," +
                "    channel VARCHAR(50) NOT NULL," +
                "    to_address VARCHAR(255) NOT NULL," +
                "    subject VARCHAR(500)," +
                "    body TEXT" +
                ")"
            };

            for (String sql : createTableStatements) {
                stmt.execute(sql);
            }

            logger.info("Tables created successfully");
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
