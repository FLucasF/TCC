package br.com.marealta.pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Pool de conexões, criação do esquema e transações. */
public class Database implements AutoCloseable {
    @FunctionalInterface
    public interface Work<T> { T run(Connection c) throws SQLException; }

    private final HikariDataSource ds;

    public Database(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(60_000); // o banco pode demorar a subir junto com o sistema
        ds = new HikariDataSource(cfg);
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
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria o que falta no banco (tabelas e os cinco quartos). Seguro com várias instâncias subindo juntas. */
    public void migrate() {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(true);
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_lock(727001)");
                try {
                    s.execute("""
                            CREATE TABLE IF NOT EXISTS rooms (
                              number INT PRIMARY KEY,
                              type TEXT NOT NULL,
                              capacity INT NOT NULL,
                              nightly_rate NUMERIC(12,2) NOT NULL)""");
                    s.execute("""
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
                              nights INT NOT NULL,
                              subtotal NUMERIC(12,2) NOT NULL,
                              discount NUMERIC(12,2) NOT NULL,
                              total NUMERIC(12,2) NOT NULL,
                              status TEXT NOT NULL,
                              amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
                              refund NUMERIC(12,2),
                              CHECK (check_out > check_in))""");
                    s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in, check_out)");
                    s.execute("CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations(check_in)");
                    s.execute("""
                            CREATE TABLE IF NOT EXISTS messages (
                              id BIGSERIAL PRIMARY KEY,
                              channel TEXT NOT NULL,
                              recipient TEXT NOT NULL,
                              subject TEXT NOT NULL,
                              body TEXT NOT NULL,
                              status TEXT NOT NULL,
                              guest_key TEXT,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now())""");
                    s.execute("CREATE INDEX IF NOT EXISTS messages_guest_idx ON messages(guest_key, created_at)");
                    s.execute("""
                            INSERT INTO rooms(number, type, capacity, nightly_rate) VALUES
                              (101, 'STANDARD', 2, 249.90),
                              (102, 'STANDARD', 2, 249.90),
                              (201, 'DELUXE', 3, 389.90),
                              (202, 'DELUXE', 3, 389.90),
                              (301, 'SUITE', 4, 579.90)
                            ON CONFLICT (number) DO NOTHING""");
                } finally {
                    s.execute("SELECT pg_advisory_unlock(727001)");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao preparar o banco: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() { ds.close(); }
}
