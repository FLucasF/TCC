package br.com.marealta.pousada.store;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {
    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS rooms (
                number       INTEGER PRIMARY KEY,
                type         VARCHAR(20) NOT NULL,
                capacity     INTEGER NOT NULL,
                nightly_rate NUMERIC(12,2) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS reservations (
                id           BIGSERIAL PRIMARY KEY,
                room_number  INTEGER NOT NULL REFERENCES rooms(number),
                guest_name   VARCHAR(200) NOT NULL,
                guest_email  VARCHAR(200) NOT NULL,
                guest_cpf    VARCHAR(30) NOT NULL,
                check_in     DATE NOT NULL,
                check_out    DATE NOT NULL,
                guests       INTEGER NOT NULL,
                rate_plan    VARCHAR(20) NOT NULL,
                nights       INTEGER NOT NULL,
                subtotal     NUMERIC(12,2) NOT NULL,
                discount     NUMERIC(12,2) NOT NULL,
                total        NUMERIC(12,2) NOT NULL,
                status       VARCHAR(20) NOT NULL,
                amount_paid  NUMERIC(12,2) NOT NULL,
                refund       NUMERIC(12,2)
            );
            CREATE TABLE IF NOT EXISTS messages (
                id        BIGSERIAL PRIMARY KEY,
                channel   VARCHAR(20) NOT NULL,
                recipient VARCHAR(300) NOT NULL,
                subject   VARCHAR(500) NOT NULL,
                body      TEXT NOT NULL,
                status    VARCHAR(20) NOT NULL
            );
            INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                (101, 'STANDARD', 2, 249.90),
                (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE', 3, 389.90),
                (202, 'DELUXE', 3, 389.90),
                (301, 'SUITE', 4, 579.90)
            ON CONFLICT (number) DO NOTHING;
            """;

    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

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

    /** Cria o que o sistema precisa; um lock evita que duas instâncias subindo juntas se atropelem. */
    public void migrate() {
        inTransaction(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(727001)");
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
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
