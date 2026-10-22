package br.com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Pool de conexões, criação do esquema e transações. */
public final class Database implements AutoCloseable {
    private final HikariDataSource ds;

    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public Database(String url, String user, String password) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        if (user != null) hc.setUsername(user);
        if (password != null) hc.setPassword(password);
        hc.setMaximumPoolSize(10);
        hc.setInitializationFailTimeout(-1);
        this.ds = new HikariDataSource(hc);
    }

    /** Cria o que falta no banco; espera o banco ficar disponível se preciso. */
    public void migrate() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                tx(c -> {
                    try (Statement s = c.createStatement()) {
                        // evita corrida entre instâncias subindo juntas
                        s.execute("SELECT pg_advisory_xact_lock(727001)");
                        s.execute(SCHEMA);
                        s.execute("""
                                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                                  (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
                                  (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90),
                                  (301, 'SUITE', 4, 579.90)
                                ON CONFLICT (number) DO NOTHING""");
                    }
                    return null;
                });
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Não foi possível preparar o banco de dados", last);
    }

    public <T> T tx(Work<T> work) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Throwable t) {
                c.rollback();
                throw t;
            }
        }
    }

    @Override
    public void close() {
        ds.close();
    }

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS rooms (
              number INTEGER PRIMARY KEY,
              type VARCHAR(20) NOT NULL,
              capacity INTEGER NOT NULL,
              nightly_rate NUMERIC(12,2) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS reservations (
              id BIGSERIAL PRIMARY KEY,
              room_number INTEGER NOT NULL REFERENCES rooms(number),
              guest_name TEXT NOT NULL,
              guest_email TEXT NOT NULL,
              guest_cpf TEXT NOT NULL,
              check_in DATE NOT NULL,
              check_out DATE NOT NULL,
              guests INTEGER NOT NULL,
              rate_plan VARCHAR(20) NOT NULL,
              nights INTEGER NOT NULL,
              subtotal NUMERIC(14,2) NOT NULL,
              discount NUMERIC(14,2) NOT NULL,
              total NUMERIC(14,2) NOT NULL,
              status VARCHAR(20) NOT NULL,
              amount_paid NUMERIC(14,2) NOT NULL DEFAULT 0,
              refund NUMERIC(14,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in, check_out);
            CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations (check_in);
            CREATE TABLE IF NOT EXISTS messages (
              id BIGSERIAL PRIMARY KEY,
              channel VARCHAR(20) NOT NULL,
              recipient TEXT NOT NULL,
              subject TEXT NOT NULL,
              body TEXT NOT NULL,
              status VARCHAR(20) NOT NULL,
              guest_address TEXT NOT NULL,
              copy BOOLEAN NOT NULL,
              created_at TIMESTAMPTZ NOT NULL
            );
            CREATE INDEX IF NOT EXISTS messages_guest_idx ON messages (lower(guest_address), created_at);
            """;
}
