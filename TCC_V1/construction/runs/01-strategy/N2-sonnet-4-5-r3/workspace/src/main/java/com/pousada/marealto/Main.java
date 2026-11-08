package com.pousada.marealto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.marealto.api.*;
import com.pousada.marealto.database.DatabaseManager;
import com.pousada.marealto.repository.MessageRepository;
import com.pousada.marealto.repository.ReservationRepository;
import com.pousada.marealto.repository.RoomRepository;
import com.pousada.marealto.service.MessageService;
import com.pousada.marealto.service.ReportService;
import com.pousada.marealto.service.ReservationService;
import com.pousada.marealto.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DatabaseManager dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);
        dbManager.initializeSchema();

        RoomRepository roomRepository = new RoomRepository(dbManager);
        ReservationRepository reservationRepository = new ReservationRepository(dbManager);
        MessageRepository messageRepository = new MessageRepository(dbManager);

        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, messageService
        );
        ReportService reportService = new ReportService(reservationRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, messageService, objectMapper
        );
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
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

        System.out.println("Pousada Maré Alta API running on port " + port);
    }
}
