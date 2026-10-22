package com.marealta.pousada.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final int ATTEMPTS = 30;

    private Database() {
    }

    /** Conecta (esperando o banco subir, se preciso) e cria o que faltar. */
    public static DataSource connect(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setInitializationFailTimeout(-1);
        HikariDataSource dataSource = new HikariDataSource(config);
        for (int attempt = 1; ; attempt++) {
            try {
                createSchema(dataSource);
                return dataSource;
            } catch (SQLException e) {
                if (attempt >= ATTEMPTS) {
                    throw new IllegalStateException("Não foi possível preparar o banco de dados", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }
        }
    }

    public static void createSchema(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INTEGER NOT NULL,
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(64) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INTEGER NOT NULL,
                        rate_plan VARCHAR(32) NOT NULL,
                        nights INTEGER NOT NULL,
                        subtotal NUMERIC(12,2) NOT NULL,
                        discount NUMERIC(12,2) NOT NULL,
                        total NUMERIC(12,2) NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        amount_paid NUMERIC(12,2) NOT NULL,
                        refund NUMERIC(12,2)
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(32) NOT NULL,
                        recipient VARCHAR(512) NOT NULL,
                        subject VARCHAR(1024) NOT NULL,
                        body TEXT NOT NULL,
                        status VARCHAR(32) NOT NULL
                    )""");
        }
    }
}
