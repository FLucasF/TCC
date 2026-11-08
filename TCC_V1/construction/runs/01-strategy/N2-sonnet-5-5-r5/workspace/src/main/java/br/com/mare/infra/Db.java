package br.com.mare.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {
    private final String url;
    private final String user;
    private final String password;

    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    public <T> T transaction(Work<T> work) {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    /** Cria o que o sistema precisa; espera o banco aceitar conexões. */
    public void migrate() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                transaction(c -> {
                    try (Statement s = c.createStatement()) {
                        s.execute("SELECT pg_advisory_xact_lock(1)");
                        s.execute("""
                                CREATE TABLE IF NOT EXISTS reservations (
                                  id SERIAL PRIMARY KEY,
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
                                  refund NUMERIC(12,2)
                                )""");
                        s.execute("""
                                CREATE TABLE IF NOT EXISTS messages (
                                  id SERIAL PRIMARY KEY,
                                  channel TEXT NOT NULL,
                                  recipient TEXT NOT NULL,
                                  subject TEXT NOT NULL,
                                  body TEXT NOT NULL
                                )""");
                    }
                    return null;
                });
                return;
            } catch (IllegalStateException e) {
                if (!(e.getCause() instanceof SQLException se)) throw e;
                last = se;
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new IllegalStateException("Banco indisponível", last);
    }
}
