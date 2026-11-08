package pousada.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Conexões e transações. Dentro de uma transação, todo acesso na mesma thread usa a mesma conexão. */
public final class Database implements AutoCloseable {
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
    }

    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    public <T> T use(SqlWork<T> work) {
        Connection existing = current.get();
        if (existing != null) {
            return run(work, existing);
        }
        try (Connection connection = dataSource.getConnection()) {
            return run(work, connection);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
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
            } catch (Throwable t) {
                connection.rollback();
                throw t;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private <T> T run(SqlWork<T> work, Connection connection) {
        try {
            return work.run(connection);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
