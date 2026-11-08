package pousada.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final int CONNECT_ATTEMPTS = 30;

    private Database() {
    }

    public static DataSource connect(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(-1);
        HikariDataSource dataSource = new HikariDataSource(config);
        createSchema(dataSource);
        return dataSource;
    }

    private static void createSchema(DataSource dataSource) {
        for (int attempt = 1; ; attempt++) {
            try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_lock(1)");
                try {
                    s.execute("""
                            CREATE TABLE IF NOT EXISTS reservations (
                                id BIGSERIAL PRIMARY KEY,
                                room_number INT NOT NULL,
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
                                refund NUMERIC(12,2)
                            )""");
                    s.execute("""
                            CREATE TABLE IF NOT EXISTS messages (
                                id BIGSERIAL PRIMARY KEY,
                                channel TEXT NOT NULL,
                                recipient TEXT NOT NULL,
                                subject TEXT NOT NULL,
                                body TEXT NOT NULL
                            )""");
                } finally {
                    s.execute("SELECT pg_advisory_unlock(1)");
                }
                return;
            } catch (SQLException e) {
                if (attempt >= CONNECT_ATTEMPTS) {
                    throw new IllegalStateException("Não foi possível preparar o banco", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
    }
}
