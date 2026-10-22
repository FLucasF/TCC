package br.com.marealta.reservas.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/** Acesso ao PostgreSQL: uma transação por operação, compartilhada pelos repositórios da mesma thread. */
public final class Database {
    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS rooms (
                number        INTEGER PRIMARY KEY,
                type          TEXT NOT NULL,
                capacity      INTEGER NOT NULL,
                nightly_rate  NUMERIC(12,2) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS reservations (
                id            BIGSERIAL PRIMARY KEY,
                room_number   INTEGER NOT NULL REFERENCES rooms(number),
                guest_name    TEXT NOT NULL,
                guest_email   TEXT NOT NULL,
                guest_cpf     TEXT NOT NULL,
                check_in      DATE NOT NULL,
                check_out     DATE NOT NULL,
                guests        INTEGER NOT NULL,
                rate_plan     TEXT NOT NULL,
                nights        INTEGER NOT NULL,
                subtotal      NUMERIC(12,2) NOT NULL,
                discount      NUMERIC(12,2) NOT NULL,
                total         NUMERIC(12,2) NOT NULL,
                status        TEXT NOT NULL,
                amount_paid   NUMERIC(12,2) NOT NULL,
                refund        NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in);
            CREATE TABLE IF NOT EXISTS messages (
                id         BIGSERIAL PRIMARY KEY,
                channel    TEXT NOT NULL,
                recipient  TEXT NOT NULL,
                subject    TEXT NOT NULL,
                body       TEXT NOT NULL
            );
            """;

    private final String url;
    private final String user;
    private final String password;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Cria as tabelas, esperando o banco ficar disponível se ele ainda estiver subindo. */
    public void initialize() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (Connection connection = DriverManager.getConnection(url, user, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT pg_advisory_lock(7070)");
                try {
                    statement.execute(SCHEMA);
                } finally {
                    statement.execute("SELECT pg_advisory_unlock(7070)");
                }
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
        throw new DatabaseException("Não foi possível preparar o banco de dados", last);
    }

    public Connection connection() {
        Connection connection = current.get();
        if (connection == null) {
            throw new IllegalStateException("Sem transação ativa");
        }
        return connection;
    }

    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro no banco de dados", e);
        }
    }
}
