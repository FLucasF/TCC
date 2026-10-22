package pousada.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Executa o trabalho numa transação: confirma se terminar, desfaz se falhar. */
    public <T> T transaction(Work<T> work) {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
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
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria o que o sistema precisa num banco vazio, esperando o banco ficar disponível. */
    public void initialize() {
        for (int attempt = 1; ; attempt++) {
            try {
                transaction(c -> {
                    try (var st = c.createStatement()) {
                        st.execute(Schema.DDL);
                    }
                    return null;
                });
                return;
            } catch (IllegalStateException e) {
                if (attempt >= 30) {
                    throw e;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}
