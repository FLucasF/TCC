package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.postgresql.ds.PGSimpleDataSource;

public final class Database {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final PGSimpleDataSource dataSource = new PGSimpleDataSource();

    public Database(String url, String user, String password) {
        dataSource.setUrl(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
    }

    /** Runs the work in one transaction: commits if it finishes, rolls back if it throws. */
    public <T> T inTransaction(Work<T> work) {
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
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Creates what the system needs in an empty database, waiting for the database to come up. */
    public void initialize() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                createSchema();
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Could not prepare the database", last);
    }

    private void createSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement st = connection.createStatement()) {
            st.execute("SELECT pg_advisory_lock(7070)");
            try {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                          number INT PRIMARY KEY,
                          type VARCHAR(20) NOT NULL,
                          capacity INT NOT NULL,
                          nightly_rate NUMERIC(12,2) NOT NULL)""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                          id BIGSERIAL PRIMARY KEY,
                          room_number INT NOT NULL REFERENCES rooms(number),
                          guest_name TEXT NOT NULL,
                          guest_email TEXT NOT NULL,
                          guest_cpf TEXT NOT NULL,
                          check_in DATE NOT NULL,
                          check_out DATE NOT NULL,
                          guests INT NOT NULL,
                          rate_plan VARCHAR(20) NOT NULL,
                          nights INT NOT NULL,
                          subtotal NUMERIC(12,2) NOT NULL,
                          discount NUMERIC(12,2) NOT NULL,
                          total NUMERIC(12,2) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          amount_paid NUMERIC(12,2) NOT NULL,
                          refund NUMERIC(12,2))""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                          id BIGSERIAL PRIMARY KEY,
                          channel VARCHAR(20) NOT NULL,
                          recipient TEXT NOT NULL,
                          subject TEXT NOT NULL,
                          body TEXT NOT NULL,
                          status VARCHAR(20) NOT NULL)""");
                st.execute("""
                        INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                          (101, 'STANDARD', 2, 249.90),
                          (102, 'STANDARD', 2, 249.90),
                          (201, 'DELUXE', 3, 389.90),
                          (202, 'DELUXE', 3, 389.90),
                          (301, 'SUITE', 4, 579.90)
                        ON CONFLICT (number) DO NOTHING""");
            } finally {
                st.execute("SELECT pg_advisory_unlock(7070)");
            }
        }
    }
}
