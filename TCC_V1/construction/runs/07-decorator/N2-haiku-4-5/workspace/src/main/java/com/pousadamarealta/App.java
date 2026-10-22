package com.pousadamarealta;

import com.pousadamarealta.api.ApiRouter;
import com.pousadamarealta.db.Database;
import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.db.ReservationRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Missing environment variables: DB_URL, DB_USER, DB_PASSWORD");
            System.exit(1);
        }

        Database database = Database.getInstance(dbUrl, dbUser, dbPassword);
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

        Javalin app = Javalin.create()
            .start(port);

        ApiRouter apiRouter = new ApiRouter(
            app, roomRepository, reservationRepository, messageRepository,
            pricingService, reservationService, messageService, reportService
        );
        apiRouter.setupRoutes();

        System.out.println("Server running on port " + port);
    }
}
