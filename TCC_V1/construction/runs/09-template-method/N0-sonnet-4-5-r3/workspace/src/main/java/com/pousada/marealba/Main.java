package com.pousada.marealba;

import com.pousada.marealba.config.Database;
import com.pousada.marealba.controller.*;
import com.pousada.marealba.exception.ApiException;
import com.pousada.marealba.repository.*;
import com.pousada.marealba.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portEnv = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database configuration not provided");
        }

        int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomService);

        RoomController roomController = new RoomController(roomService);
        MessageController messageController = new MessageController(messageService);
        ReservationController reservationController = new ReservationController(reservationService);
        ReportController reportController = new ReportController(reportService);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
        }).start(port);

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
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);

        app.exception(ApiException.class, (e, ctx) -> {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getErrorCode().name());
            error.put("message", e.getMessage());
            ctx.status(e.getHttpStatus()).json(error);
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "INTERNAL_ERROR");
            error.put("message", "An unexpected error occurred");
            ctx.status(500).json(error);
        });

        System.out.println("Pousada Maré Alta system started on port " + port);
    }
}
