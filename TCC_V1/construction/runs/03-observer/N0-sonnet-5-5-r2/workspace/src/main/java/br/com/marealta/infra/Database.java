package br.com.marealta.infra;

import br.com.marealta.service.Transactions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/** Pool de conexões, transações e criação do esquema. A transação corrente fica presa à thread. */
public final class Database implements Transactions, AutoCloseable {
    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS rooms (
                number       INTEGER PRIMARY KEY,
                type         VARCHAR(20)   NOT NULL,
                capacity     INTEGER       NOT NULL,
                nightly_rate NUMERIC(12,2) NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS reservations (
                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                room_number INTEGER       NOT NULL REFERENCES rooms(number),
                guest_name  VARCHAR(255)  NOT NULL,
                guest_email VARCHAR(255)  NOT NULL,
                guest_cpf   VARCHAR(32)   NOT NULL,
                check_in    DATE          NOT NULL,
                check_out   DATE          NOT NULL,
                guests      INTEGER       NOT NULL,
                rate_plan   VARCHAR(20)   NOT NULL,
                nights      INTEGER       NOT NULL,
                subtotal    NUMERIC(12,2) NOT NULL,
                discount    NUMERIC(12,2) NOT NULL,
                total       NUMERIC(12,2) NOT NULL,
                status      VARCHAR(20)   NOT NULL,
                amount_paid NUMERIC(12,2) NOT NULL,
                refund      NUMERIC(12,2)
            )""",
            "CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)",
            "CREATE INDEX IF NOT EXISTS reservations_check_in ON reservations (check_in)",
            """
            CREATE TABLE IF NOT EXISTS messages (
                id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                channel   VARCHAR(20)  NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject   VARCHAR(255) NOT NULL,
                body      TEXT         NOT NULL
            )""",
            """
            INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                (101, 'STANDARD', 2, 249.90),
                (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE',   3, 389.90),
                (202, 'DELUXE',   3, 389.90),
                (301, 'SUITE',    4, 579.90)
            ON CONFLICT (number) DO UPDATE
                SET type = EXCLUDED.type, capacity = EXCLUDED.capacity, nightly_rate = EXCLUDED.nightly_rate"""
    };

    private final HikariDataSource dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(-1); // o banco pode demorar a ficar de pé; tentamos em initSchema
        this.dataSource = new HikariDataSource(config);
    }

    /** Cria o que falta no banco, esperando-o ficar disponível por até ~60s. */
    public void initSchema() {
        SQLException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try (Connection c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                try (Statement st = c.createStatement()) {
                    // Evita corrida entre duas instâncias subindo juntas.
                    st.execute("SELECT pg_advisory_xact_lock(7070001)");
                    for (String ddl : SCHEMA) {
                        st.execute(ddl);
                    }
                }
                c.commit();
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Não foi possível preparar o banco de dados", last);
    }

    Connection connection() {
        Connection c = current.get();
        if (c == null) {
            throw new IllegalStateException("Acesso ao banco fora de uma transação");
        }
        return c;
    }

    @Override
    public <T> T run(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            current.set(c);
            try {
                T result = work.get();
                c.commit();
                return result;
            } catch (Throwable t) {
                try {
                    c.rollback();
                } catch (SQLException suppressed) {
                    t.addSuppressed(suppressed);
                }
                throw t;
            } finally {
                current.remove();
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
