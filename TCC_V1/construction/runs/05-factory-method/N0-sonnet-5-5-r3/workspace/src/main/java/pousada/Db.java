package pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db implements AutoCloseable {
    private final HikariDataSource ds;

    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    public Db(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1); // a conexão é validada em init(), com tentativas
        this.ds = new HikariDataSource(cfg);
    }

    /** Espera o banco ficar disponível e cria o que faltar (banco começa vazio). */
    public void init() {
        long deadline = System.currentTimeMillis() + 60_000;
        while (true) {
            try {
                tx(c -> {
                    try (Statement st = c.createStatement()) {
                        // serializa a criação se vários processos sobem juntos
                        st.execute("SELECT pg_advisory_xact_lock(727274)");
                        st.execute(SCHEMA);
                        st.execute("""
                                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                                  (101, 'STANDARD', 2, 249.90),
                                  (102, 'STANDARD', 2, 249.90),
                                  (201, 'DELUXE',   3, 389.90),
                                  (202, 'DELUXE',   3, 389.90),
                                  (301, 'SUITE',    4, 579.90)
                                ON CONFLICT (number) DO NOTHING""");
                    }
                    return null;
                });
                return;
            } catch (SQLException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("Banco de dados indisponível: " + e.getMessage(), e);
                }
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public <T> T tx(TxWork<T> work) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public void close() {
        ds.close();
    }

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS rooms (
              number       INTEGER PRIMARY KEY,
              type         TEXT NOT NULL,
              capacity     INTEGER NOT NULL,
              nightly_rate NUMERIC(12,2) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS reservations (
              id             BIGSERIAL PRIMARY KEY,
              room_number    INTEGER NOT NULL REFERENCES rooms(number),
              guest_name     TEXT NOT NULL,
              guest_email    TEXT NOT NULL,
              guest_cpf      TEXT,
              company_cnpj   TEXT,
              company_name   TEXT,
              guest_passport TEXT,
              guest_country  TEXT,
              check_in       DATE NOT NULL,
              check_out      DATE NOT NULL,
              guests         INTEGER NOT NULL,
              rate_plan      TEXT NOT NULL,
              nights         INTEGER NOT NULL,
              subtotal       NUMERIC(12,2) NOT NULL,
              discount       NUMERIC(12,2) NOT NULL,
              total          NUMERIC(12,2) NOT NULL,
              status         TEXT NOT NULL,
              amount_paid    NUMERIC(12,2) NOT NULL,
              refund         NUMERIC(12,2)
            );
            CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in, check_out);
            CREATE TABLE IF NOT EXISTS messages (
              id        BIGSERIAL PRIMARY KEY,
              channel   TEXT NOT NULL,
              recipient TEXT NOT NULL,
              subject   TEXT NOT NULL,
              body      TEXT NOT NULL
            );""";
}
