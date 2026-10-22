package pousada.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database implements AutoCloseable {
    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INT NOT NULL,
                guest_name TEXT NOT NULL,
                guest_email TEXT NOT NULL,
                identity_kind TEXT NOT NULL,
                identity_data TEXT NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INT NOT NULL,
                rate_plan TEXT NOT NULL,
                nights INT NOT NULL,
                subtotal NUMERIC(14,2) NOT NULL,
                discount NUMERIC(14,2) NOT NULL,
                total NUMERIC(14,2) NOT NULL,
                status TEXT NOT NULL,
                amount_paid NUMERIC(14,2) NOT NULL,
                refund NUMERIC(14,2)
            )""",
            "CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)",
            "CREATE INDEX IF NOT EXISTS reservations_check_in ON reservations (check_in)",
            """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel TEXT NOT NULL,
                recipient TEXT NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            )"""
    };

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final HikariDataSource dataSource;

    private Database(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Conecta (esperando o banco subir, se preciso) e cria o que faltar. */
    public static Database connect(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        Database database = new Database(new HikariDataSource(config));
        database.migrate();
        return database;
    }

    private void migrate() {
        tx(connection -> {
            try (Statement statement = connection.createStatement()) {
                // Serializa a criação do schema caso duas instâncias subam juntas.
                statement.execute("SELECT pg_advisory_xact_lock(7070)");
                for (String ddl : SCHEMA) {
                    statement.execute(ddl);
                }
            }
            return null;
        });
    }

    public <T> T tx(Work<T> work) {
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
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
