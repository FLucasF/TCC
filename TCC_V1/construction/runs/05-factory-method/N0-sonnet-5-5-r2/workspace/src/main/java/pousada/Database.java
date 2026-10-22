package pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Connection pool, schema creation and transaction helper. */
public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000); // the database may still be starting
        this.dataSource = new HikariDataSource(config);
        createSchema();
    }

    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    public <T> T inTransaction(TxWork<T> work) {
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
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private void createSchema() {
        inTransaction(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(7070001)"); // several instances may start together
                s.execute("""
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
                        )""");
                s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in)");
                s.execute("""
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
