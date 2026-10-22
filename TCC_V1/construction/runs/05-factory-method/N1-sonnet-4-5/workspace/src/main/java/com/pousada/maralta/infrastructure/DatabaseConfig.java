package com.pousada.maralta.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    public static void initialize() {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/maralta");
        String user = System.getenv().getOrDefault("DB_USER", "postgres");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);

        createTables();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static void createTables() {
        try (Connection conn = dataSource.getConnection();
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
                    guest_cpf VARCHAR(14),
                    company_cnpj VARCHAR(18),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL
                )
            """);

            initializeRooms(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    private static void initializeRooms(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('101', 'STANDARD') ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('102', 'STANDARD') ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('201', 'DELUXE') ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('202', 'DELUXE') ON CONFLICT DO NOTHING");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('301', 'SUITE') ON CONFLICT DO NOTHING");
        }
    }
}
