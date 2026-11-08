package br.com.mare.pousada.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Db implements AutoCloseable {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INT NOT NULL,
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
                refund NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out);
            CREATE INDEX IF NOT EXISTS reservations_check_in ON reservations (check_in);
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel TEXT NOT NULL,
                recipient TEXT NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            );
            """;

    private final HikariDataSource dataSource;

    public Db(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
        createSchema();
    }

    private void createSchema() {
        inTransaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(0)");
                statement.execute(SCHEMA);
            }
            return null;
        });
    }

    public <T> T inTransaction(Work<T> work) {
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

    @Override
    public void close() {
        dataSource.close();
    }
}
