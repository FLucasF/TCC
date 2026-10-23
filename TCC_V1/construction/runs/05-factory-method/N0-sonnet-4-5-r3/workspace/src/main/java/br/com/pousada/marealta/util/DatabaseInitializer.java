package br.com.pousada.marealta.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseInitializer(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void initialize() {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20),
                    company_cnpj VARCHAR(20),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL,
                    refund DECIMAL(10, 2)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """);

            stmt.execute("SELECT COUNT(*) FROM rooms");
            var rs = stmt.getResultSet();
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('101', 'STANDARD')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('102', 'STANDARD')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('201', 'DELUXE')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('202', 'DELUXE')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('301', 'SUITE')");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
