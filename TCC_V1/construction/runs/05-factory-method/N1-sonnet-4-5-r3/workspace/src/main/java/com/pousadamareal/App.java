package com.pousadamareal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamareal.controller.*;
import com.pousadamareal.repository.*;
import com.pousadamareal.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portEnv = System.getenv("PORT");
        int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", roomController::listRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::listMessages);

        app.get("/reports/occupancy", reportController::occupancyReport);
        app.get("/reports/revenue", reportController::revenueReport);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Server started on port " + port);
    }
}
