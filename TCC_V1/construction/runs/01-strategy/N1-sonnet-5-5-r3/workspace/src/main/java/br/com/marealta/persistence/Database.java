package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {
    /** Trabalho feito dentro de uma transação. */
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS reservations (
                id           BIGSERIAL PRIMARY KEY,
                room_number  INT NOT NULL,
                guest_name   TEXT NOT NULL,
                guest_email  TEXT NOT NULL,
                guest_cpf    TEXT NOT NULL,
                check_in     DATE NOT NULL,
                check_out    DATE NOT NULL,
                guests       INT NOT NULL,
                rate_plan    TEXT NOT NULL,
                company_code TEXT,
                nights       INT NOT NULL,
                subtotal     NUMERIC(12,2) NOT NULL,
                discount     NUMERIC(12,2) NOT NULL,
                status       TEXT NOT NULL,
                amount_paid  NUMERIC(12,2) NOT NULL,
                refund       NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in, check_out);
            CREATE TABLE IF NOT EXISTS messages (
                id      BIGSERIAL PRIMARY KEY,
                channel TEXT NOT NULL,
                to_addr TEXT NOT NULL,
                subject TEXT NOT NULL,
                body    TEXT NOT NULL
            );
            """;

    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
    }

    public void createSchema() {
        tx(connection -> {
            try (Statement statement = connection.createStatement()) {
                // Evita corrida entre duas instâncias subindo juntas.
                statement.execute("SELECT pg_advisory_xact_lock(7070)");
                statement.execute(SCHEMA);
            }
            return null;
        });
    }

    public <T> T tx(Work<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Throwable e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }
}
