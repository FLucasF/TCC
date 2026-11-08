package pousada.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public <T> T inTransaction(Function<Connection, T> work) {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Cria o que falta no banco; espera o banco subir se preciso. */
    public void initialize() {
        for (int attempt = 1; ; attempt++) {
            try {
                inTransaction(c -> {
                    try (Statement s = c.createStatement()) {
                        s.execute("SELECT pg_advisory_xact_lock(7070)");
                        s.execute("""
                                CREATE TABLE IF NOT EXISTS rooms (
                                  number INTEGER PRIMARY KEY,
                                  type TEXT NOT NULL,
                                  capacity INTEGER NOT NULL,
                                  nightly_rate NUMERIC(12,2) NOT NULL)""");
                        s.execute("""
                                CREATE TABLE IF NOT EXISTS reservations (
                                  id BIGSERIAL PRIMARY KEY,
                                  room_number INTEGER NOT NULL REFERENCES rooms(number),
                                  guest_name TEXT NOT NULL,
                                  guest_email TEXT NOT NULL,
                                  guest_cpf TEXT NOT NULL,
                                  check_in DATE NOT NULL,
                                  check_out DATE NOT NULL,
                                  guests INTEGER NOT NULL,
                                  rate_plan TEXT NOT NULL,
                                  nights INTEGER NOT NULL,
                                  subtotal NUMERIC(12,2) NOT NULL,
                                  discount NUMERIC(12,2) NOT NULL,
                                  total NUMERIC(12,2) NOT NULL,
                                  status TEXT NOT NULL,
                                  amount_paid NUMERIC(12,2) NOT NULL,
                                  refund NUMERIC(12,2))""");
                        s.execute("""
                                CREATE TABLE IF NOT EXISTS messages (
                                  id BIGSERIAL PRIMARY KEY,
                                  channel TEXT NOT NULL,
                                  recipient TEXT NOT NULL,
                                  subject TEXT NOT NULL,
                                  body TEXT NOT NULL)""");
                        s.execute("""
                                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                                  (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
                                  (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90),
                                  (301, 'SUITE', 4, 579.90)
                                ON CONFLICT (number) DO NOTHING""");
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                    return null;
                });
                return;
            } catch (IllegalStateException e) {
                if (attempt >= 30) throw e;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}
