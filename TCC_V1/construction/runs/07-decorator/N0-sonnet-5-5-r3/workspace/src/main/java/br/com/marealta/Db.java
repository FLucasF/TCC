package br.com.marealta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Acesso ao PostgreSQL: conexões, transações e criação do esquema. */
public final class Db {
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private final String url, user, password;

    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public <T> T tx(Work<T> work) {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Throwable t) {
                c.rollback();
                throw t;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria o que faltar; espera o banco ficar disponível (até ~60s). */
    public void init() {
        for (int attempt = 1; ; attempt++) {
            try {
                tx(c -> {
                    try (Statement st = c.createStatement()) {
                        st.execute("SELECT pg_advisory_xact_lock(7070)");
                        st.execute("""
                            CREATE TABLE IF NOT EXISTS rooms (
                              number INT PRIMARY KEY,
                              type VARCHAR(20) NOT NULL,
                              capacity INT NOT NULL,
                              nightly_rate NUMERIC(12,2) NOT NULL)""");
                        st.execute("""
                            CREATE TABLE IF NOT EXISTS reservations (
                              id BIGSERIAL PRIMARY KEY,
                              room_number INT NOT NULL REFERENCES rooms(number),
                              guest_name TEXT NOT NULL,
                              guest_email TEXT NOT NULL,
                              guest_cpf TEXT NOT NULL,
                              check_in DATE NOT NULL,
                              check_out DATE NOT NULL,
                              guests INT NOT NULL,
                              rate_plan VARCHAR(20) NOT NULL,
                              nights INT NOT NULL,
                              subtotal NUMERIC(12,2) NOT NULL,
                              discount NUMERIC(12,2) NOT NULL,
                              total NUMERIC(12,2) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              amount_paid NUMERIC(12,2) NOT NULL,
                              refund NUMERIC(12,2))""");
                        st.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in)");
                        st.execute("""
                            CREATE TABLE IF NOT EXISTS messages (
                              id BIGSERIAL PRIMARY KEY,
                              channel VARCHAR(20) NOT NULL,
                              to_addr TEXT NOT NULL,
                              subject TEXT NOT NULL,
                              body TEXT NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              guest_address TEXT,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now())""");
                        st.execute("CREATE INDEX IF NOT EXISTS messages_guest_idx ON messages(guest_address, created_at)");
                        st.execute("""
                            INSERT INTO rooms(number, type, capacity, nightly_rate) VALUES
                              (101,'STANDARD',2,249.90),(102,'STANDARD',2,249.90),
                              (201,'DELUXE',3,389.90),(202,'DELUXE',3,389.90),
                              (301,'SUITE',4,579.90)
                            ON CONFLICT (number) DO NOTHING""");
                    }
                    return null;
                });
                return;
            } catch (RuntimeException e) {
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
