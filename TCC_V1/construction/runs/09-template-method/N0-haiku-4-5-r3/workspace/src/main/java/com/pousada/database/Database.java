package com.pousada.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
    }

    public void init() {
        try (Connection conn = dataSource.getConnection()) {
            createSchema(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    private void createSchema(Connection conn) throws SQLException {
        String[] statements = {
                "CREATE TABLE IF NOT EXISTS rooms (" +
                        "number INTEGER PRIMARY KEY," +
                        "type VARCHAR(20) NOT NULL," +
                        "capacity INTEGER NOT NULL," +
                        "nightly_rate NUMERIC(10, 2) NOT NULL" +
                        ")",

                "CREATE TABLE IF NOT EXISTS reservations (" +
                        "id SERIAL PRIMARY KEY," +
                        "room_number INTEGER NOT NULL," +
                        "guest_name VARCHAR(255) NOT NULL," +
                        "guest_email VARCHAR(255) NOT NULL," +
                        "guest_cpf VARCHAR(14) NOT NULL," +
                        "check_in DATE NOT NULL," +
                        "check_out DATE NOT NULL," +
                        "guests INTEGER NOT NULL," +
                        "rate_plan VARCHAR(20) NOT NULL," +
                        "nights INTEGER NOT NULL," +
                        "subtotal NUMERIC(10, 2) NOT NULL," +
                        "discount NUMERIC(10, 2) NOT NULL," +
                        "total NUMERIC(10, 2) NOT NULL," +
                        "status VARCHAR(20) NOT NULL," +
                        "amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0," +
                        "refund NUMERIC(10, 2)," +
                        "FOREIGN KEY (room_number) REFERENCES rooms(number)" +
                        ")",

                "CREATE TABLE IF NOT EXISTS messages (" +
                        "id SERIAL PRIMARY KEY," +
                        "channel VARCHAR(20) NOT NULL," +
                        "\"to\" VARCHAR(255) NOT NULL," +
                        "subject VARCHAR(500) NOT NULL," +
                        "body TEXT NOT NULL" +
                        ")",

                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90) " +
                        "ON CONFLICT DO NOTHING",
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90) " +
                        "ON CONFLICT DO NOTHING",
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90) " +
                        "ON CONFLICT DO NOTHING",
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90) " +
                        "ON CONFLICT DO NOTHING",
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90) " +
                        "ON CONFLICT DO NOTHING"
        };

        for (String sql : statements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        dataSource.close();
    }
}
