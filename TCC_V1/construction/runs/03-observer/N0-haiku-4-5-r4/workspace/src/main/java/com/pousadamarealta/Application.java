package com.pousadamarealta;

import com.google.gson.Gson;
import com.pousadamarealta.api.ApiRoutes;
import com.pousadamarealta.database.DatabaseConfig;
import com.pousadamarealta.repositories.MessageRepository;
import com.pousadamarealta.repositories.ReservationRepository;
import com.pousadamarealta.repositories.RoomRepository;
import com.pousadamarealta.services.*;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
            System.exit(1);
        }

        DatabaseConfig dbConfig = new DatabaseConfig(dbUrl, dbUser, dbPassword);
        dbConfig.initializeDatabase();
        dbConfig.seedRooms();

        RoomRepository roomRepository = new RoomRepository(dbConfig);
        ReservationRepository reservationRepository = new ReservationRepository(dbConfig);
        MessageRepository messageRepository = new MessageRepository(dbConfig);

        PricingService pricingService = new PricingService();
        NotificationService notificationService = new NotificationService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            pricingService,
            notificationService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        Gson gson = new Gson();
        ApiRoutes apiRoutes = new ApiRoutes(
            roomRepository,
            reservationRepository,
            messageRepository,
            reservationService,
            notificationService,
            reportService,
            gson
        );

        Javalin app = Javalin.create()
            .start(port);

        apiRoutes.registerRoutes(app);

        System.out.println("Pousada Maré Alta API listening on port " + port);
    }
}
