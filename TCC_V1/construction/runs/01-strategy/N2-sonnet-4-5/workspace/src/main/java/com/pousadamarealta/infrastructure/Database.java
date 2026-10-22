package com.pousadamarealta.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public class Database {
    private static HikariDataSource dataSource;

    public static DataSource initialize(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
        createTables();

        return dataSource;
    }

    private static void createTables() {
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
            """;

        String createReservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                room_type VARCHAR(20) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan_type VARCHAR(30) NOT NULL,
                nights BIGINT NOT NULL,
                subtotal DECIMAL(10, 2) NOT NULL,
                discount DECIMAL(10, 2) NOT NULL,
                total DECIMAL(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL,
                refund DECIMAL(10, 2)
            )
            """;

        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500),
                body TEXT NOT NULL
            )
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createRoomsTable);
            stmt.execute(createReservationsTable);
            stmt.execute(createMessagesTable);

        } catch (Exception e) {
            throw new RuntimeException("Error creating database tables", e);
        }
    }

    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
