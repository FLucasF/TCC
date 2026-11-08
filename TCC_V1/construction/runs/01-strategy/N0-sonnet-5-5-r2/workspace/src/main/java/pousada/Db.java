package pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private Db() {}

    /** Abre o pool, esperando o banco ficar disponível (ele pode subir junto com o sistema). */
    public static HikariDataSource connect(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1);
        HikariDataSource ds = new HikariDataSource(cfg);
        long deadline = System.currentTimeMillis() + 60_000;
        while (true) {
            try (Connection c = ds.getConnection()) {
                return ds;
            } catch (SQLException e) {
                if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Banco indisponível", e);
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    /** Cria o que faltar no banco e garante os cinco quartos. Seguro para rodar a cada subida. */
    public static void migrate(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            st.execute("SELECT pg_advisory_xact_lock(7070001)");
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
                    rate_plan VARCHAR(40) NOT NULL,
                    company_code VARCHAR(40),
                    nights INT NOT NULL,
                    subtotal NUMERIC(12,2) NOT NULL,
                    discount NUMERIC(12,2) NOT NULL,
                    total NUMERIC(12,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
                    refund NUMERIC(12,2))""");
            st.execute("CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    body TEXT NOT NULL)""");
            st.execute("""
                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                    (101, 'STANDARD', 2, 249.90),
                    (102, 'STANDARD', 2, 249.90),
                    (201, 'DELUXE', 3, 389.90),
                    (202, 'DELUXE', 3, 389.90),
                    (301, 'SUITE', 4, 579.90)
                ON CONFLICT (number) DO NOTHING""");
            c.commit();
        }
    }
}
