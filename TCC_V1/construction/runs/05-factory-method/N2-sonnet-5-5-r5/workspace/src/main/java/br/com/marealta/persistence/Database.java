package br.com.marealta.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

public final class Database {
    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T value) throws SQLException;
    }

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INT NOT NULL,
                guest_name TEXT NOT NULL,
                guest_email TEXT NOT NULL,
                guest_cpf TEXT,
                company_cnpj TEXT,
                company_name TEXT,
                guest_passport TEXT,
                guest_country TEXT,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INT NOT NULL,
                rate_plan TEXT NOT NULL,
                nights INT NOT NULL,
                subtotal NUMERIC(12,2) NOT NULL,
                discount NUMERIC(12,2) NOT NULL,
                total NUMERIC(12,2) NOT NULL,
                status TEXT NOT NULL,
                amount_paid NUMERIC(12,2) NOT NULL,
                refund NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in);
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel TEXT NOT NULL,
                recipient TEXT NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            );
            """;

    private final HikariDataSource dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(8);
        config.setInitializationFailTimeout(-1);
        this.dataSource = new HikariDataSource(config);
    }

    /** Cria o que o sistema precisa; espera o banco ficar disponível. */
    public void initSchema() {
        SQLException last = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
                s.execute(SCHEMA);
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Banco indisponível", last);
    }

    public <R> R withConnection(SqlFunction<Connection, R> work) {
        Connection existing = current.get();
        try {
            if (existing != null) {
                return work.apply(existing);
            }
            try (Connection c = dataSource.getConnection()) {
                return work.apply(c);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Tudo o que os repositórios fizerem dentro de {@code work} vale junto ou não vale. */
    public <R> R inTransaction(Supplier<R> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            current.set(c);
            try {
                R result = work.get();
                c.commit();
                return result;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void close() {
        dataSource.close();
    }
}
