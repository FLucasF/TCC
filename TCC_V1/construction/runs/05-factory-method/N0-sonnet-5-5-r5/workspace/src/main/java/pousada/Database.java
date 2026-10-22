package pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class Database {
    private Database() {}

    /** Conecta (esperando o banco subir, se preciso) e cria o esquema. */
    public static DataSource connect(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1);
        HikariDataSource ds = new HikariDataSource(cfg);
        SQLException last = null;
        for (int i = 0; i < 60; i++) {
            try (Connection c = ds.getConnection()) {
                createSchema(c);
                return ds;
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
        throw new IllegalStateException("Banco de dados indisponível", last);
    }

    private static void createSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            // serializa a criação caso várias instâncias subam juntas
            s.execute("SELECT pg_advisory_lock(727001)");
            try {
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INT NOT NULL,
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
                            rate_plan TEXT NOT NULL,
                            nights INT NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status TEXT NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2)
                        )""");
                s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in, check_out)");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel TEXT NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL
                        )""");
            } finally {
                s.execute("SELECT pg_advisory_unlock(727001)");
            }
        }
    }
}
