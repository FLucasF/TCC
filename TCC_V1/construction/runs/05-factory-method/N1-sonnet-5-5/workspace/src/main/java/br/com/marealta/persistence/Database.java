package br.com.marealta.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS rooms (
                number       INTEGER PRIMARY KEY,
                type         VARCHAR(20) NOT NULL,
                capacity     INTEGER NOT NULL,
                nightly_rate NUMERIC(12,2) NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS reservations (
                id            BIGSERIAL PRIMARY KEY,
                room_number   INTEGER NOT NULL REFERENCES rooms(number),
                guest_name    VARCHAR(200) NOT NULL,
                guest_email   VARCHAR(200) NOT NULL,
                identity_kind VARCHAR(20) NOT NULL,
                identity_data TEXT NOT NULL,
                check_in      DATE NOT NULL,
                check_out     DATE NOT NULL,
                guests        INTEGER NOT NULL,
                rate_plan     VARCHAR(20) NOT NULL,
                nights        INTEGER NOT NULL,
                subtotal      NUMERIC(12,2) NOT NULL,
                discount      NUMERIC(12,2) NOT NULL,
                total         NUMERIC(12,2) NOT NULL,
                status        VARCHAR(20) NOT NULL,
                amount_paid   NUMERIC(12,2) NOT NULL,
                refund        NUMERIC(12,2)
            )""",
            "CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)",
            """
            CREATE TABLE IF NOT EXISTS messages (
                id        BIGSERIAL PRIMARY KEY,
                channel   VARCHAR(20) NOT NULL,
                recipient VARCHAR(200) NOT NULL,
                subject   VARCHAR(300) NOT NULL,
                body      TEXT NOT NULL
            )"""
    };

    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
    }

    /** Cria o que falta no banco e grava os quartos da pousada. */
    public void initialize(RoomRepository rooms) {
        inTransaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(7070)");
                for (String ddl : SCHEMA) {
                    statement.execute(ddl);
                }
            }
            rooms.seed(connection);
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
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    public void close() {
        dataSource.close();
    }
}
