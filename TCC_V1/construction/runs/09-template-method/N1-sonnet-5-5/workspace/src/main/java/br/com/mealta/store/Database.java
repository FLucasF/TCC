package br.com.mealta.store;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/** Pool de conexões do PostgreSQL e transações presas à thread que as abriu. */
public class Database implements Transactor, AutoCloseable {
    @FunctionalInterface
    public interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final HikariDataSource pool;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        this.pool = new HikariDataSource(config);
    }

    /** Usa a conexão da transação em curso; sem transação, pega uma só para esta chamada. */
    public <T> T withConnection(SqlWork<T> work) {
        Connection inTransaction = current.get();
        try {
            if (inTransaction != null) {
                return work.apply(inTransaction);
            }
            try (Connection connection = pool.getConnection()) {
                return work.apply(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    @Override
    public <T> T run(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException | Error e) {
                connection.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
