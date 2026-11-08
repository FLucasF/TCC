package br.com.pousadamarealta.reservas.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/** Pool de conexões e transações. Os repositórios usam a conexão da transação em andamento. */
public class Database implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        this(url, user, password, null);
    }

    public Database(String url, String user, String password, String schema) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        if (schema != null) {
            config.setSchema(schema);
        }
        config.setMaximumPoolSize(10);
        // o banco pode demorar a ficar de pé quando tudo sobe junto
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
    }

    public Connection connection() {
        Connection connection = current.get();
        if (connection == null) {
            throw new IllegalStateException("acesso ao banco fora de uma transação");
        }
        return connection;
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
            } catch (RuntimeException | Error e) {
                try {
                    connection.rollback();
                } catch (SQLException suppressed) {
                    e.addSuppressed(suppressed);
                }
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
