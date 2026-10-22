package pousada.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {
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
                rate_plan VARCHAR(20) NOT NULL,
                subtotal NUMERIC(12,2) NOT NULL,
                discount NUMERIC(12,2) NOT NULL,
                total NUMERIC(12,2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid NUMERIC(12,2) NOT NULL,
                refund NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out);
            CREATE INDEX IF NOT EXISTS reservations_check_in ON reservations (check_in);
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient TEXT NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            );
            """;

    private final HikariDataSource dataSource;

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
        inTransaction(c -> {
            try (Statement s = c.createStatement()) {
                // evita corrida entre instâncias subindo juntas
                s.execute("SELECT pg_advisory_xact_lock(-1)");
                s.execute(SCHEMA);
            }
            return null;
        });
    }

    public <T> T inTransaction(Work<T> work) {
        try (Connection c = dataSource.getConnection()) {
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
            throw new PersistenceException(e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
