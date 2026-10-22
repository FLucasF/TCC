package com.marealta.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
    }

    public <T> T transaction(Work<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    public void migrate() {
        transaction(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(7070)");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            number INTEGER PRIMARY KEY,
                            type VARCHAR(20) NOT NULL,
                            capacity INTEGER NOT NULL,
                            nightly_rate NUMERIC(12,2) NOT NULL
                        )""");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INTEGER NOT NULL REFERENCES rooms(number),
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf TEXT NOT NULL,
                            check_in DATE NOT NULL,
                            check_out DATE NOT NULL,
                            guests INTEGER NOT NULL,
                            rate_plan VARCHAR(20) NOT NULL,
                            nights INTEGER NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2)
                        )""");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel VARCHAR(20) NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL,
                            status VARCHAR(20) NOT NULL
                        )""");
                s.execute("""
                        INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                            (101, 'STANDARD', 2, 249.90),
                            (102, 'STANDARD', 2, 249.90),
                            (201, 'DELUXE', 3, 389.90),
                            (202, 'DELUXE', 3, 389.90),
                            (301, 'SUITE', 4, 579.90)
                        ON CONFLICT (number) DO NOTHING""");
            }
            return null;
        });
    }
}
