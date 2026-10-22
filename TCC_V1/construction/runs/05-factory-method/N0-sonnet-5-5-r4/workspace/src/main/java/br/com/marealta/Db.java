package br.com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/** Conexão com o PostgreSQL e criação do esquema (o banco começa vazio). */
public final class Db {
    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS rooms (
            number INT PRIMARY KEY,
            type VARCHAR(20) NOT NULL,
            capacity INT NOT NULL,
            nightly_rate NUMERIC(12,2) NOT NULL)
        """,
        """
        CREATE TABLE IF NOT EXISTS reservations (
            id BIGSERIAL PRIMARY KEY,
            room_number INT NOT NULL REFERENCES rooms(number),
            guest_name TEXT NOT NULL,
            guest_email TEXT NOT NULL,
            guest_cpf TEXT,
            company_cnpj TEXT,
            company_name TEXT,
            guest_passport TEXT,
            guest_country TEXT,
            check_in DATE NOT NULL,
            check_out DATE NOT NULL,
            guests INT NOT NULL,
            rate_plan VARCHAR(20) NOT NULL,
            nights INT NOT NULL,
            subtotal NUMERIC(12,2) NOT NULL,
            discount NUMERIC(12,2) NOT NULL,
            total NUMERIC(12,2) NOT NULL,
            status VARCHAR(20) NOT NULL,
            amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
            refund NUMERIC(12,2))
        """,
        "CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)",
        """
        CREATE TABLE IF NOT EXISTS messages (
            id BIGSERIAL PRIMARY KEY,
            channel VARCHAR(20) NOT NULL,
            to_addr TEXT NOT NULL,
            subject TEXT NOT NULL,
            body TEXT NOT NULL)
        """,
        """
        INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
            (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
            (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90),
            (301, 'SUITE', 4, 579.90)
        ON CONFLICT (number) DO NOTHING
        """
    };

    private Db() {}

    public static DataSource connect(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1); // não falha na criação do pool; tentamos abaixo
        HikariDataSource ds = new HikariDataSource(cfg);
        long deadline = System.currentTimeMillis() + 60_000;
        while (true) {
            try {
                init(ds);
                return ds;
            } catch (SQLException e) {
                if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Banco indisponível", e);
                System.err.println("Aguardando o banco: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
    }

    static void init(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                // Serializa a criação do esquema caso várias instâncias subam juntas.
                st.execute("SELECT pg_advisory_xact_lock(727401)");
                for (String sql : SCHEMA) st.execute(sql);
            }
            c.commit();
        }
    }
}
