package pousada.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public class Database implements AutoCloseable {
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

    /** Cria o que falta no banco; pode rodar mais de uma vez e por mais de um processo. */
    public void createSchema() {
        inTransaction(tx -> {
            tx.lockSchemaCreation();
            Schema.create(tx.connection());
            return null;
        });
    }

    /** Roda o trabalho numa transação: confirma se terminar bem, desfaz se der qualquer erro. */
    public <T> T inTransaction(Function<Tx, T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(new Tx(connection));
                connection.commit();
                return result;
            } catch (RuntimeException | Error e) {
                connection.rollback();
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
