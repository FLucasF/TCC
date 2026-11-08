package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
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
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria o que o sistema precisa no banco; espera o banco aceitar conexões (até ~60s). */
    public void createSchema() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_lock(7070)");
                try {
                    s.execute("""
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
                                refund NUMERIC(12,2)
                            )""");
                    s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in)");
                    s.execute("""
                            CREATE TABLE IF NOT EXISTS messages (
                                id BIGSERIAL PRIMARY KEY,
                                channel TEXT NOT NULL,
                                recipient TEXT NOT NULL,
                                subject TEXT NOT NULL,
                                body TEXT NOT NULL
                            )""");
                } finally {
                    s.execute("SELECT pg_advisory_unlock(7070)");
                }
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Não foi possível preparar o banco de dados", last);
    }
}
