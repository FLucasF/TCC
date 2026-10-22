package br.com.marealta.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {
    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS rooms (
                number INT PRIMARY KEY,
                type TEXT NOT NULL,
                capacity INT NOT NULL,
                nightly_rate NUMERIC(12,2) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INT NOT NULL REFERENCES rooms(number),
                guest_name TEXT NOT NULL,
                guest_email TEXT NOT NULL,
                guest_cpf TEXT NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INT NOT NULL,
                rate_plan TEXT NOT NULL,
                company_code TEXT,
                nights INT NOT NULL,
                subtotal NUMERIC(12,2) NOT NULL,
                discount NUMERIC(12,2) NOT NULL,
                total NUMERIC(12,2) NOT NULL,
                status TEXT NOT NULL,
                amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
                refund NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS idx_reservations_room ON reservations (room_number, check_in);
            CREATE INDEX IF NOT EXISTS idx_reservations_check_in ON reservations (check_in);
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel TEXT NOT NULL,
                recipient TEXT NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            );
            INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                (101, 'STANDARD', 2, 249.90),
                (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE', 3, 389.90),
                (202, 'DELUXE', 3, 389.90),
                (301, 'SUITE', 4, 579.90)
            ON CONFLICT (number) DO NOTHING;
            """;

    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    private final HikariDataSource ds;

    public Database(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1); // o banco pode demorar a subir; o init abaixo tenta de novo
        this.ds = new HikariDataSource(cfg);
    }

    /** Cria as tabelas (e os quartos) se ainda não existirem; espera o banco ficar disponível. */
    public void init() {
        long deadline = System.currentTimeMillis() + 60_000;
        while (true) {
            try {
                inTransaction(c -> {
                    try (Statement st = c.createStatement()) {
                        // evita corrida se duas instâncias subirem juntas
                        st.execute("SELECT pg_advisory_xact_lock(7070)");
                        st.execute(SCHEMA);
                    }
                    return null;
                });
                return;
            } catch (RuntimeException e) {
                if (System.currentTimeMillis() > deadline) throw e;
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
            }
        }
    }

    public <T> T inTransaction(TxWork<T> work) {
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
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() { ds.close(); }
}
