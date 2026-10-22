package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.controller.ApiController;
import com.pousadamarealta.messaging.MessagingService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        String testModeStr = System.getenv("HOTEL_TEST_MODE");

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;
        boolean testMode = "true".equalsIgnoreCase(testModeStr);

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration is required: DB_URL, DB_USER, DB_PASSWORD");
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        RoomService roomService = new RoomService(roomRepository);
        MessagingService messagingService = new MessagingService(messageRepository, testMode);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, messagingService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            roomService, reservationService, messagingService, reportService
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        apiController.registerRoutes(app);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.close();
        }));

        System.out.println("Pousada Maré Alta Reservation System started on port " + port);
        System.out.println("Test mode: " + testMode);
    }
}
