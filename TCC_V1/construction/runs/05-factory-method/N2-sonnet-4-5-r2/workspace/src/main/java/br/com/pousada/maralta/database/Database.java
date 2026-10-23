package br.com.pousada.maralta.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final HikariDataSource dataSource;

    public Database(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        this.dataSource = new HikariDataSource(config);
        runMigrations();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        dataSource.close();
    }

    private void runMigrations() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(200) NOT NULL,
                    guest_email VARCHAR(200) NOT NULL,
                    guest_cpf VARCHAR(14),
                    company_cnpj VARCHAR(18),
                    company_name VARCHAR(200),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INT NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(200) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
            """);

            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO rooms (number, type) VALUES ('101', 'STANDARD')");
                    stmt.execute("INSERT INTO rooms (number, type) VALUES ('102', 'STANDARD')");
                    stmt.execute("INSERT INTO rooms (number, type) VALUES ('201', 'DELUXE')");
                    stmt.execute("INSERT INTO rooms (number, type) VALUES ('202', 'DELUXE')");
                    stmt.execute("INSERT INTO rooms (number, type) VALUES ('301', 'SUITE')");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to run migrations", e);
        }
    }
}
