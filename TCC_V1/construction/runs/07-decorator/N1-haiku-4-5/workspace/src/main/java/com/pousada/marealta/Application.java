package com.pousada.marealta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.marealta.api.ReservationController;
import com.pousada.marealta.api.RoomController;
import com.pousada.marealta.api.ReportController;
import com.pousada.marealta.api.MessageController;
import com.pousada.marealta.infrastructure.persistence.*;
import com.pousada.marealta.infrastructure.messaging.MessageSender;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.service.ReportService;
import io.javalin.Javalin;

public class Application {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String port = System.getenv("PORT");
        String testMode = System.getenv("HOTEL_TEST_MODE");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database configuration missing");
        }

        int portNumber = port == null ? 7070 : Integer.parseInt(port);
        boolean testModeEnabled = "true".equalsIgnoreCase(testMode);

        Database database = new Database(dbUrl, dbUser, dbPassword);
        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        MessageSender messageSender = new MessageSender(messageRepository, testModeEnabled);
        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository, messageSender);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService, reservationRepository, roomRepository);
        ReportController reportController = new ReportController(reportService);
        MessageController messageController = new MessageController(messageRepository);

        Javalin app = Javalin.create().start(portNumber);

        roomController.register(app);
        reservationController.register(app);
        reportController.register(app);
        messageController.register(app);

        System.out.println("Server started on port " + portNumber);
    }
}
