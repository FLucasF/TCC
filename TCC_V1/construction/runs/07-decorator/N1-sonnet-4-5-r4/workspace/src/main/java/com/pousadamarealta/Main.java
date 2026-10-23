package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        DatabaseManager.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        MessageService messageService = new MessageService(messageRepository, testMode);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageService);
        ReportService reportService = new ReportService(reservationService, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
                reservationService, roomRepository);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        app.get("/rooms", roomController::getAll);
        app.post("/reservations", reservationController::create);
        app.get("/reservations/{id}", reservationController::getById);
        app.post("/reservations/{id}/confirmation", reservationController::confirm);
        app.post("/reservations/{id}/cancellation", reservationController::cancel);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getAll);
        app.get("/reports/occupancy", reportController::getOccupancy);
        app.get("/reports/revenue", reportController::getRevenue);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            DatabaseManager.close();
        }));
    }
}
