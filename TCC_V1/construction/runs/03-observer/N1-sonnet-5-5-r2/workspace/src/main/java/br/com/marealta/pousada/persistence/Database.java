package br.com.marealta.pousada.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/** Conexão com o PostgreSQL e transações. Os repositórios usam a conexão da transação em andamento. */
public class Database implements AutoCloseable {

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
                amount_paid NUMERIC(12,2) NOT NULL,
                refund NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out);
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
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
        createSchema();
    }

    private void createSchema() {
        transaction(() -> {
            try (Statement st = connection().createStatement()) {
                // evita que duas instâncias subindo juntas criem as tabelas ao mesmo tempo
                st.execute("SELECT pg_advisory_xact_lock(7070)");
                st.execute(SCHEMA);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
    }

    public Connection connection() {
        Connection connection = current.get();
        if (connection == null) {
            throw new IllegalStateException("Operação de banco fora de uma transação");
        }
        return connection;
    }

    public <T> T transaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException | Error e) {
                connection.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
