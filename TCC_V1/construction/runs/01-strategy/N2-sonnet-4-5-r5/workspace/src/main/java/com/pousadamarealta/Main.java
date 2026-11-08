package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.*;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.*;
import com.pousadamarealta.util.DatabaseManager;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration missing. Set DB_URL, DB_USER, and DB_PASSWORD");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        DatabaseManager dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);
        dbManager.initialize();

        RoomRepository roomRepository = new RoomRepository(dbManager);
        ReservationRepository reservationRepository = new ReservationRepository(dbManager);
        MessageRepository messageRepository = new MessageRepository(dbManager);

        NotificationService notificationService = new NotificationService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, notificationService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService, roomRepository, notificationService, objectMapper
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            Map<String, String> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getErrorCode());
            errorMap.put("message", e.getMessage());
            ctx.json(errorMap);
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            Map<String, String> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "INTERNAL_ERROR");
            errorMap.put("message", "An internal error occurred");
            ctx.json(errorMap);
        });

        app.get("/rooms", roomController::getRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.start(port);

        System.out.println("Pousada Maré Alta API running on port " + port);
    }
}
