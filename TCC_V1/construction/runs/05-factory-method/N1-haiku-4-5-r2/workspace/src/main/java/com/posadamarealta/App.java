package com.posadamarealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        DataSource ds = createDataSource();
        runMigrations(ds);

        Javalin app = Javalin.create();

        com.posadamarealta.handlers.RoomHandler roomHandler = new com.posadamarealta.handlers.RoomHandler(ds);
        com.posadamarealta.handlers.ReservationHandler reservationHandler = new com.posadamarealta.handlers.ReservationHandler(ds);
        com.posadamarealta.handlers.MessageHandler messageHandler = new com.posadamarealta.handlers.MessageHandler(ds);
        com.posadamarealta.handlers.ReportHandler reportHandler = new com.posadamarealta.handlers.ReportHandler(ds);

        app.get("/rooms", roomHandler::list);
        app.post("/reservations", reservationHandler::create);
        app.get("/reservations/{id}", reservationHandler::get);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirm);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancel);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);
        app.get("/messages", messageHandler::list);
        app.get("/reports/occupancy", reportHandler::occupancy);
        app.get("/reports/revenue", reportHandler::revenue);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        app.start(port);
        logger.info("Server started on port {}", port);
    }

    private static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("Missing database environment variables");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        return new HikariDataSource(config);
    }

    private static void runMigrations(DataSource ds) {
        try (Connection conn = ds.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate NUMERIC(10,2) NOT NULL
                )
                """);

            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20),
                    company_cnpj VARCHAR(30),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10,2) NOT NULL,
                    discount NUMERIC(10,2) NOT NULL,
                    total NUMERIC(10,2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10,2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    "to" VARCHAR(255) NOT NULL,
                    subject VARCHAR(255),
                    body TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            conn.createStatement().execute("""
                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                (101, 'STANDARD', 2, 249.90),
                (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE', 3, 389.90),
                (202, 'DELUXE', 3, 389.90),
                (301, 'SUITE', 4, 579.90)
                ON CONFLICT (number) DO NOTHING
                """);

            logger.info("Migrations completed");
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed", e);
        }
    }
}
