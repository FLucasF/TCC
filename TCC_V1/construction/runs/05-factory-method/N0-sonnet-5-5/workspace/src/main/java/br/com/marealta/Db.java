package br.com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

final class Db {
    private final DataSource ds;

    Db(DataSource ds) { this.ds = ds; }

    interface Work<T> { T run(Connection c) throws SQLException; }

    static Db connect(String url, String user, String password) throws InterruptedException {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1);
        HikariDataSource ds = new HikariDataSource(cfg);
        Db db = new Db(ds);
        // o banco pode demorar a ficar de pé junto com o sistema
        for (int attempt = 1; ; attempt++) {
            try {
                db.initSchema();
                return db;
            } catch (SQLException e) {
                if (attempt >= 30) throw new IllegalStateException("Banco indisponível: " + e.getMessage(), e);
                Thread.sleep(2000);
            }
        }
    }

    /** Executa o trabalho numa transação. */
    <T> T tx(Work<T> work) {
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
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initSchema() throws SQLException {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            s.execute("SELECT pg_advisory_xact_lock(727401)");
            s.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                  number INTEGER PRIMARY KEY,
                  type VARCHAR(20) NOT NULL,
                  capacity INTEGER NOT NULL,
                  nightly_rate NUMERIC(12,2) NOT NULL)""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                  id BIGSERIAL PRIMARY KEY,
                  room_number INTEGER NOT NULL REFERENCES rooms(number),
                  guest_name TEXT NOT NULL,
                  guest_email TEXT NOT NULL,
                  guest_cpf TEXT,
                  company_cnpj TEXT,
                  company_name TEXT,
                  guest_passport TEXT,
                  guest_country TEXT,
                  check_in DATE NOT NULL,
                  check_out DATE NOT NULL,
                  guests INTEGER NOT NULL,
                  rate_plan VARCHAR(20) NOT NULL,
                  nights INTEGER NOT NULL,
                  subtotal NUMERIC(12,2) NOT NULL,
                  discount NUMERIC(12,2) NOT NULL,
                  total NUMERIC(12,2) NOT NULL,
                  status VARCHAR(20) NOT NULL,
                  amount_paid NUMERIC(12,2) NOT NULL,
                  refund NUMERIC(12,2))""");
            s.execute("CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations(room_number, check_in, check_out)");
            s.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                  id BIGSERIAL PRIMARY KEY,
                  channel VARCHAR(20) NOT NULL,
                  recipient TEXT NOT NULL,
                  subject TEXT NOT NULL,
                  body TEXT NOT NULL)""");
            s.execute("""
                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                  (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
                  (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90),
                  (301, 'SUITE', 4, 579.90)
                ON CONFLICT (number) DO NOTHING""");
            c.commit();
        }
    }
}
