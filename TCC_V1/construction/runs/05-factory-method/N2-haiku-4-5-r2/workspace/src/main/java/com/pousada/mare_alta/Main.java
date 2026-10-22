package com.pousada.mare_alta;

import com.pousada.mare_alta.database.Database;
import com.pousada.mare_alta.http.AppRoutes;
import com.pousada.mare_alta.repository.MessageRepository;
import com.pousada.mare_alta.repository.ReservationRepository;
import com.pousada.mare_alta.repository.RoomRepository;
import com.pousada.mare_alta.service.ReportService;
import com.pousada.mare_alta.service.ReservationService;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) throws Exception {
        // Load environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Missing database environment variables: DB_URL, DB_USER, DB_PASSWORD");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        // Initialize database
        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        // Initialize repositories
        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        // Initialize services
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        // Setup routes
        AppRoutes appRoutes = new AppRoutes(
                reservationService, reportService, roomRepository, messageRepository);

        // Create and start Javalin app
        Javalin app = Javalin.create().start(port);
        appRoutes.setupRoutes(app);

        System.out.println("Server started on port " + port);
    }
}
