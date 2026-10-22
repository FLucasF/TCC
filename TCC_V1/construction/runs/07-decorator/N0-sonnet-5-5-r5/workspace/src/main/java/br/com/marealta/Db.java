package br.com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class Db {
    public interface TxFn<T> {
        T apply(Connection c) throws SQLException;
    }

    private final DataSource ds;

    public Db(DataSource ds) {
        this.ds = ds;
    }

    public static Db connect(Config cfg) throws InterruptedException {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(cfg.dbUrl());
        hc.setUsername(cfg.dbUser());
        hc.setPassword(cfg.dbPassword());
        hc.setMaximumPoolSize(10);
        hc.setInitializationFailTimeout(-1);
        HikariDataSource ds = new HikariDataSource(hc);
        Db db = new Db(ds);
        // o banco pode ainda estar subindo: tenta por até ~60s
        for (int i = 0; ; i++) {
            try {
                db.initSchema();
                return db;
            } catch (RuntimeException e) {
                if (i >= 30) throw e;
                Thread.sleep(2000);
            }
        }
    }

    public <T> T tx(TxFn<T> fn) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T r = fn.apply(c);
                c.commit();
                return r;
            } catch (Throwable t) {
                c.rollback();
                throw t;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void initSchema() {
        tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(727001)");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                          number INT PRIMARY KEY,
                          type TEXT NOT NULL,
                          capacity INT NOT NULL,
                          nightly_rate NUMERIC(12,2) NOT NULL)""");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS counters (
                          name TEXT PRIMARY KEY,
                          value BIGINT NOT NULL)""");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                          id BIGINT PRIMARY KEY,
                          room_number INT NOT NULL REFERENCES rooms(number),
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
                s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in)");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                          id BIGINT PRIMARY KEY,
                          channel TEXT NOT NULL,
                          to_addr TEXT NOT NULL,
                          subject TEXT NOT NULL,
                          body TEXT NOT NULL,
                          status TEXT NOT NULL,
                          original_to TEXT NOT NULL,
                          copy BOOLEAN NOT NULL,
                          sent_on DATE NOT NULL)""");
                s.execute("CREATE INDEX IF NOT EXISTS messages_limit_idx ON messages(original_to, sent_on)");
                s.execute("""
                        INSERT INTO rooms(number, type, capacity, nightly_rate) VALUES
                          (101,'STANDARD',2,249.90),(102,'STANDARD',2,249.90),
                          (201,'DELUXE',3,389.90),(202,'DELUXE',3,389.90),
                          (301,'SUITE',4,579.90)
                        ON CONFLICT (number) DO NOTHING""");
                s.execute("INSERT INTO counters(name, value) VALUES ('reservations',0),('messages',0) ON CONFLICT (name) DO NOTHING");
            }
            return null;
        });
    }
}
