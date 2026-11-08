package br.marealta.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static Database instance;
    private final String url;
    private final String user;
    private final String password;

    private Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            String url = System.getenv("DB_URL");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");

            if (url == null || user == null || password == null) {
                throw new IllegalStateException("Database environment variables not set");
            }

            instance = new Database(url, user, password);
            instance.initialize();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "number INT PRIMARY KEY," +
                    "type VARCHAR(50) NOT NULL," +
                    "capacity INT NOT NULL," +
                    "nightly_rate NUMERIC(10, 2) NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                    "id SERIAL PRIMARY KEY," +
                    "room_number INT NOT NULL REFERENCES rooms(number)," +
                    "guest_name VARCHAR(255) NOT NULL," +
                    "guest_email VARCHAR(255) NOT NULL," +
                    "guest_cpf VARCHAR(14) NOT NULL," +
                    "check_in DATE NOT NULL," +
                    "check_out DATE NOT NULL," +
                    "guests INT NOT NULL," +
                    "rate_plan VARCHAR(50) NOT NULL," +
                    "nights INT NOT NULL," +
                    "subtotal NUMERIC(10, 2) NOT NULL," +
                    "discount NUMERIC(10, 2) NOT NULL," +
                    "total NUMERIC(10, 2) NOT NULL," +
                    "status VARCHAR(50) NOT NULL," +
                    "amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0," +
                    "refund NUMERIC(10, 2)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "id SERIAL PRIMARY KEY," +
                    "channel VARCHAR(50) NOT NULL," +
                    "to_address VARCHAR(255) NOT NULL," +
                    "subject VARCHAR(255) NOT NULL," +
                    "body TEXT NOT NULL" +
                    ")");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_room ON reservations(room_number)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_check_in ON reservations(check_in)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status)");

            initializeRooms(stmt);

            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException(e);
        }
    }

    private void initializeRooms(Statement stmt) throws SQLException {
        stmt.execute("DELETE FROM rooms");

        stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90)");
        stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90)");
        stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90)");
        stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90)");
        stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90)");
    }
}
