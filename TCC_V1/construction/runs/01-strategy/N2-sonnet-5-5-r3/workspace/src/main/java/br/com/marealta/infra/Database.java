package br.com.marealta.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private final String url, user, password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void migrate() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                inTransaction(c -> {
                    try (Statement st = c.createStatement()) {
                        st.execute("""
                                CREATE TABLE IF NOT EXISTS reservations (
                                  id BIGSERIAL PRIMARY KEY,
                                  room_number INT NOT NULL,
                                  guest_name TEXT NOT NULL,
                                  guest_email TEXT NOT NULL,
                                  guest_cpf TEXT NOT NULL,
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
            } catch (RuntimeException e) {
                if (!(e.getCause() instanceof SQLException se)) throw e;
                last = se;
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new IllegalStateException("Banco indisponível", last);
    }

    public <T> T inTransaction(Work<T> work) {
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
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
