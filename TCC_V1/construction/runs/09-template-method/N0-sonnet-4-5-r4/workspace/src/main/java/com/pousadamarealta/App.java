package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.handler.MessageHandler;
import com.pousadamarealta.handler.ReportHandler;
import com.pousadamarealta.handler.ReservationHandler;
import com.pousadamarealta.handler.RoomHandler;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DatabaseManager dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);
        dbManager.initialize();

        RoomRepository roomRepository = new RoomRepository(dbManager);
        ReservationRepository reservationRepository = new ReservationRepository(dbManager);
        MessageRepository messageRepository = new MessageRepository(dbManager);

        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageRepository, pricingService);
        ReportService reportService = new ReportService(
            reservationRepository, roomRepository, pricingService);

        RoomHandler roomHandler = new RoomHandler(roomRepository);
        ReservationHandler reservationHandler = new ReservationHandler(
            reservationService, reservationRepository, roomRepository);
        MessageHandler messageHandler = new MessageHandler(messageRepository);
        ReportHandler reportHandler = new ReportHandler(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", roomHandler::getAllRooms);

        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);

        app.get("/messages", messageHandler::getAllMessages);

        app.get("/reports/occupancy", reportHandler::getOccupancyReport);
        app.get("/reports/revenue", reportHandler::getRevenueReport);
        app.get("/reports/tourism-tax", reportHandler::getTourismTaxReport);
        app.get("/reports/rate-plans", reportHandler::getRatePlansReport);

        System.out.println("Pousada Maré Alta reservation system running on port " + port);
    }
}
