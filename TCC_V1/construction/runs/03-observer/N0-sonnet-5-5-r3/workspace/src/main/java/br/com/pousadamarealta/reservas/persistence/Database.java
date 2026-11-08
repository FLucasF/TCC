package br.com.pousadamarealta.reservas.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Conexões, transações e criação do esquema. */
public class Database implements AutoCloseable {
    private static final int CONNECT_ATTEMPTS = 30;

    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(-1);
        this.dataSource = new HikariDataSource(config);
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
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
            throw new IllegalStateException("Erro no banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria o que falta no banco (que começa vazio) e cadastra os quartos que ainda não existem. */
    public void migrate() {
        SQLException last = null;
        for (int attempt = 1; attempt <= CONNECT_ATTEMPTS; attempt++) {
            try {
                createSchema();
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
        throw new IllegalStateException("Não foi possível preparar o banco de dados", last);
    }

    private void createSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                // evita corrida entre instâncias subindo ao mesmo tempo
                st.execute("SELECT pg_advisory_xact_lock(7070001)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            number       INTEGER PRIMARY KEY,
                            type         TEXT NOT NULL,
                            capacity     INTEGER NOT NULL,
                            nightly_rate NUMERIC(12,2) NOT NULL
                        )""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id          BIGSERIAL PRIMARY KEY,
                            room_number INTEGER NOT NULL REFERENCES rooms(number),
                            guest_name  TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf   TEXT NOT NULL,
                            check_in    DATE NOT NULL,
                            check_out   DATE NOT NULL,
                            guests      INTEGER NOT NULL,
                            rate_plan   TEXT NOT NULL,
                            nights      INTEGER NOT NULL,
                            subtotal    NUMERIC(12,2) NOT NULL,
                            discount    NUMERIC(12,2) NOT NULL,
                            total       NUMERIC(12,2) NOT NULL,
                            status      TEXT NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund      NUMERIC(12,2)
                        )""");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id        BIGSERIAL PRIMARY KEY,
                            channel   TEXT NOT NULL,
                            recipient TEXT NOT NULL,
                            subject   TEXT NOT NULL,
                            body      TEXT NOT NULL
                        )""");
                st.execute("""
                        INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                            (101, 'STANDARD', 2, 249.90),
                            (102, 'STANDARD', 2, 249.90),
                            (201, 'DELUXE',   3, 389.90),
                            (202, 'DELUXE',   3, 389.90),
                            (301, 'SUITE',    4, 579.90)
                        ON CONFLICT (number) DO NOTHING""");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
