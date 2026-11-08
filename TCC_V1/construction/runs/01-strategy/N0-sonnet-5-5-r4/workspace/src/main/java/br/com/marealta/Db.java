package br.com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Pool de conexões, criação do esquema e transações. */
public final class Db {
    @FunctionalInterface
    public interface TxWork<T> { T run(Connection c) throws SQLException; }

    private final HikariDataSource ds;

    public Db(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1);
        this.ds = new HikariDataSource(cfg);
    }

    public <T> T tx(TxWork<T> work) {
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
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    /** Espera o banco aceitar conexões, cria as tabelas e cadastra os quartos. */
    public void init() {
        SQLException last = null;
        for (int i = 0; i < 60; i++) {
            try (Connection c = ds.getConnection()) {
                migrate(c);
                return;
            } catch (SQLException e) {
                last = e;
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw new IllegalStateException("Banco de dados indisponível", last);
    }

    private void migrate(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try (Statement s = c.createStatement()) {
            // evita corrida entre instâncias subindo juntas
            s.execute("SELECT pg_advisory_xact_lock(727401)");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INT PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INT NOT NULL,
                        nightly_rate NUMERIC(12,2) NOT NULL)""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INT NOT NULL REFERENCES rooms(number),
                        guest_name TEXT NOT NULL,
                        guest_email TEXT NOT NULL,
                        guest_cpf TEXT NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        rate_plan VARCHAR(30) NOT NULL,
                        company_code VARCHAR(60),
                        nights INT NOT NULL,
                        subtotal NUMERIC(12,2) NOT NULL,
                        discount NUMERIC(12,2) NOT NULL,
                        total NUMERIC(12,2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount_paid NUMERIC(12,2) NOT NULL,
                        refund NUMERIC(12,2))""");
            s.execute("CREATE INDEX IF NOT EXISTS idx_reservations_room ON reservations(room_number, check_in)");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(20) NOT NULL,
                        recipient TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        body TEXT NOT NULL)""");
        }
        String seed = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?,?,?,?) ON CONFLICT (number) DO NOTHING";
        try (PreparedStatement ps = c.prepareStatement(seed)) {
            Object[][] rooms = {
                    {101, "STANDARD", 2, "249.90"}, {102, "STANDARD", 2, "249.90"},
                    {201, "DELUXE", 3, "389.90"}, {202, "DELUXE", 3, "389.90"},
                    {301, "SUITE", 4, "579.90"}};
            for (Object[] r : rooms) {
                ps.setInt(1, (Integer) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setInt(3, (Integer) r[2]);
                ps.setBigDecimal(4, new java.math.BigDecimal((String) r[3]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        c.commit();
    }
}
