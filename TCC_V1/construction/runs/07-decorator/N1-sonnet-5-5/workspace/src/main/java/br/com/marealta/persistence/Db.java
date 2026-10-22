package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;

    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public <T> T transaction(Work<T> work) {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }
}
