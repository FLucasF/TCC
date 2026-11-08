package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.controller.*;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null) {
            throw new RuntimeException("DB_URL environment variable is required");
        }
        if (dbUser == null) {
            throw new RuntimeException("DB_USER environment variable is required");
        }
        if (dbPassword == null) {
            throw new RuntimeException("DB_PASSWORD environment variable is required");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomService roomService = new RoomService(database);
        MessageService messageService = new MessageService(database);
        ReservationService reservationService = new ReservationService(database, roomService, messageService);
        ReportService reportService = new ReportService(database);

        RoomController roomController = new RoomController(roomService);
        MessageController messageController = new MessageController(messageService);
        ReservationController reservationController = new ReservationController(reservationService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.exception(ApiException.class, (e, ctx) -> {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getErrorCode());
            error.put("message", e.getMessage());
            ctx.status(e.getStatusCode());
            ctx.json(error);
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "INTERNAL_ERROR");
            error.put("message", "An internal error occurred");
            ctx.status(500);
            ctx.json(error);
        });

        app.get("/rooms", roomController::getAllRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAllMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        System.out.println("Pousada Maré Alta - Sistema de Reservas iniciado na porta " + port);
    }
}
