package com.pousadamarealta;

import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;

import javax.sql.DataSource;

public class App {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL", null);
        String dbUser = getEnv("DB_USER", null);
        String dbPassword = getEnv("DB_PASSWORD", null);
        int port = Integer.parseInt(getEnv("PORT", "7070"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration is required: DB_URL, DB_USER, DB_PASSWORD");
        }

        DataSource dataSource = createDataSource(dbUrl, dbUser, dbPassword);

        DatabaseInitializer dbInitializer = new DatabaseInitializer(dataSource);
        dbInitializer.initialize();

        RoomRepository roomRepository = new JdbcRoomRepository(dataSource);
        roomRepository.initialize();

        ReservationRepository reservationRepository = new JdbcReservationRepository(dataSource);
        MessageRepository messageRepository = new JdbcMessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageRepository, pricingService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ReservationController controller = new ReservationController(
            reservationService, reportService, roomRepository, messageRepository);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(port);

        app.get("/rooms", controller::getRooms);
        app.post("/reservations", controller::createReservation);
        app.get("/reservations/{id}", controller::getReservation);
        app.post("/reservations/{id}/confirmation", controller::confirmReservation);
        app.post("/reservations/{id}/cancellation", controller::cancelReservation);
        app.post("/reservations/{id}/check-in", controller::checkIn);
        app.post("/reservations/{id}/check-out", controller::checkOut);
        app.get("/reservations/{id}/document", controller::getDocument);
        app.get("/messages", controller::getMessages);
        app.get("/reports/occupancy", controller::getOccupancyReport);
        app.get("/reports/revenue", controller::getRevenueReport);
        app.get("/reports/tourism-tax", controller::getTourismTaxReport);
        app.get("/reports/rate-plans", controller::getRatePlansReport);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }

    private static DataSource createDataSource(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
