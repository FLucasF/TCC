package pousada.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
    public <T> T tx(Work<T> work) {
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
            throw new IllegalStateException("Erro no banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria o que o sistema precisa, esperando o banco ficar disponível se for preciso. */
    public void createSchema() {
        for (int attempt = 1; ; attempt++) {
            try {
                tx(connection -> {
                    try (Statement st = connection.createStatement()) {
                        st.execute("SELECT pg_advisory_xact_lock(1)");
                        st.execute("""
                                CREATE TABLE IF NOT EXISTS reservations (
                                  id BIGSERIAL PRIMARY KEY,
                                  room_number INT NOT NULL,
                                  guest_name TEXT NOT NULL,
                                  guest_email TEXT NOT NULL,
                                  identity_type TEXT NOT NULL,
                                  identity_data TEXT NOT NULL,
                                  check_in DATE NOT NULL,
                                  check_out DATE NOT NULL,
                                  guests INT NOT NULL,
                                  rate_plan TEXT NOT NULL,
                                  nights INT NOT NULL,
                                  subtotal NUMERIC(12,2) NOT NULL,
                                  discount NUMERIC(12,2) NOT NULL,
                                  total NUMERIC(12,2) NOT NULL,
                                  status TEXT NOT NULL,
                                  amount_paid NUMERIC(12,2) NOT NULL,
                                  refund NUMERIC(12,2))""");
                        st.execute("""
                                CREATE TABLE IF NOT EXISTS messages (
                                  id BIGSERIAL PRIMARY KEY,
                                  channel TEXT NOT NULL,
                                  recipient TEXT NOT NULL,
                                  subject TEXT NOT NULL,
                                  body TEXT NOT NULL)""");
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
