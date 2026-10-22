package br.com.marealta.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Conexões com o PostgreSQL, transações e criação das tabelas. */
public class Db implements AutoCloseable {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

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
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    private void createSchema() {
        transaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(7070)");
                statement.execute("""
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
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2)
                        )""");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel TEXT NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL
                        )""");
            }
            return null;
        });
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
