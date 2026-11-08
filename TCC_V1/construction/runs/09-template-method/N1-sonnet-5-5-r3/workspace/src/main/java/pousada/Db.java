package pousada;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    @FunctionalInterface
    public interface Work<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;

    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Executa o trabalho numa transação: confirma se terminar bem, desfaz se falhar. */
    public <T> T tx(Work<T> work) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
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
}
