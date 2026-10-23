package com.pousada.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    private void initializeSchema() {
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
            """;

        String createReservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                subtotal DECIMAL(10, 2) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL,
                refund DECIMAL(10, 2)
            )
            """;

        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(20) NOT NULL
            )
            """;

        String insertRooms = """
            INSERT INTO rooms (number, type) VALUES
                ('101', 'STANDARD'),
                ('102', 'STANDARD'),
                ('201', 'DELUXE'),
                ('202', 'DELUXE'),
                ('301', 'SUITE')
            ON CONFLICT (number) DO NOTHING
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createRoomsTable);
            stmt.execute(createReservationsTable);
            stmt.execute(createMessagesTable);
            stmt.execute(insertRooms);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
