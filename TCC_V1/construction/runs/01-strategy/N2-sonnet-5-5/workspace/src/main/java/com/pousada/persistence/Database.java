package com.pousada.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public <T> T tx(Work<T> work) {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    /** Cria o que o sistema precisa no banco; espera o banco ficar disponível. */
    public void initialize() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                tx(c -> {
                    createSchema(c);
                    return null;
                });
                return;
            } catch (IllegalStateException e) {
                if (!(e.getCause() instanceof SQLException sql)) {
                    throw e;
                }
                last = sql;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Banco de dados indisponível", last);
    }

    private void createSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("SELECT pg_advisory_xact_lock(727274)");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INT PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INT NOT NULL,
                        nightly_rate NUMERIC(12,2) NOT NULL)""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INT NOT NULL REFERENCES rooms(number),
                        guest_name VARCHAR(200) NOT NULL,
                        guest_email VARCHAR(200) NOT NULL,
                        guest_cpf VARCHAR(30) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        rate_plan VARCHAR(30) NOT NULL,
                        company_code VARCHAR(50),
                        nights INT NOT NULL,
                        subtotal NUMERIC(12,2) NOT NULL,
                        discount NUMERIC(12,2) NOT NULL,
                        total NUMERIC(12,2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount_paid NUMERIC(12,2) NOT NULL,
                        refund NUMERIC(12,2))""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(20) NOT NULL,
                        recipient VARCHAR(200) NOT NULL,
                        subject VARCHAR(300) NOT NULL,
                        body TEXT NOT NULL)""");
            s.execute("""
                    INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                        (101, 'STANDARD', 2, 249.90),
                        (102, 'STANDARD', 2, 249.90),
                        (201, 'DELUXE', 3, 389.90),
                        (202, 'DELUXE', 3, 389.90),
                        (301, 'SUITE', 4, 579.90)
                    ON CONFLICT (number) DO NOTHING""");
        }
    }
}
