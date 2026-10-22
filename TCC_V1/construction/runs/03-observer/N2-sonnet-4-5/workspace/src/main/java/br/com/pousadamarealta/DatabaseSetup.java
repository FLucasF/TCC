package br.com.pousadamarealta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseSetup(String dbUrl, String dbUser, String dbPassword) {
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
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    room_type VARCHAR(20) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """);

            long roomCount = 0;
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            if (rs.next()) {
                roomCount = rs.getLong(1);
            }

            if (roomCount == 0) {
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('101', 'STANDARD')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('102', 'STANDARD')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('201', 'DELUXE')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('202', 'DELUXE')");
                stmt.execute("INSERT INTO rooms (number, type) VALUES ('301', 'SUITE')");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error initializing database", e);
        }
    }
}
