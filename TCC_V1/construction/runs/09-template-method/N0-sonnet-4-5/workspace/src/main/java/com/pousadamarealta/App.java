package com.pousadamarealta;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.ApiController;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PricingService pricingService = new PricingService();
        NotificationService notificationService = new NotificationService(messageRepository);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, pricingService, notificationService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
                roomRepository, reservationService, messageRepository, reportService);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        });

        apiController.registerRoutes(app);

        app.start(port);
        System.out.println("Pousada Maré Alta server started on port " + port);
    }
}
