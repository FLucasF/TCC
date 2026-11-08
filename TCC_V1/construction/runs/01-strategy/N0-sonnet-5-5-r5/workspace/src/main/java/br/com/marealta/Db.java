package br.com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Db implements AutoCloseable {
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private final HikariDataSource ds;

    public Db(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1); // não falha na criação; tentamos conectar em init()
        this.ds = new HikariDataSource(cfg);
    }

    /** Espera o banco ficar disponível e cria o que faltar. */
    public void init() {
        long deadline = System.currentTimeMillis() + 90_000;
        while (true) {
            try {
                tx(c -> {
                    try (Statement s = c.createStatement()) {
                        // evita corrida se duas instâncias subirem juntas
                        s.execute("SELECT pg_advisory_xact_lock(7070001)");
                        for (String sql : SCHEMA) {
                            s.execute(sql);
                        }
                    }
                    return null;
                });
                return;
            } catch (RuntimeException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                System.err.println("Aguardando o banco: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    public <T> T tx(Work<T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Throwable t) {
                c.rollback();
                throw t;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        ds.close();
    }

    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS rooms (
            number INTEGER PRIMARY KEY,
            type VARCHAR(20) NOT NULL,
            capacity INTEGER NOT NULL,
            nightly_rate NUMERIC(10,2) NOT NULL
        )""",
        """
        INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
            (101, 'STANDARD', 2, 249.90),
            (102, 'STANDARD', 2, 249.90),
            (201, 'DELUXE', 3, 389.90),
            (202, 'DELUXE', 3, 389.90),
            (301, 'SUITE', 4, 579.90)
        ON CONFLICT (number) DO NOTHING""",
        """
        CREATE TABLE IF NOT EXISTS reservations (
            id BIGSERIAL PRIMARY KEY,
            room_number INTEGER NOT NULL REFERENCES rooms(number),
            guest_name TEXT NOT NULL,
            guest_email TEXT NOT NULL,
            guest_cpf TEXT NOT NULL,
            check_in DATE NOT NULL,
            check_out DATE NOT NULL,
            guests INTEGER NOT NULL,
            rate_plan VARCHAR(30) NOT NULL,
            company_code VARCHAR(40),
            nights INTEGER NOT NULL,
            subtotal NUMERIC(12,2) NOT NULL,
            discount NUMERIC(12,2) NOT NULL,
            total NUMERIC(12,2) NOT NULL,
            status VARCHAR(20) NOT NULL,
            amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
            refund NUMERIC(12,2)
        )""",
        "CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in, check_out)",
        "CREATE INDEX IF NOT EXISTS reservations_checkin_idx ON reservations (check_in)",
        """
        CREATE TABLE IF NOT EXISTS messages (
            id BIGSERIAL PRIMARY KEY,
            channel VARCHAR(20) NOT NULL,
            recipient TEXT NOT NULL,
            subject TEXT NOT NULL,
            body TEXT NOT NULL
        )"""
    };
}
