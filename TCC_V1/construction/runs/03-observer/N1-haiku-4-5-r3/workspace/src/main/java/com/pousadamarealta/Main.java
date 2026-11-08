package com.pousadamarealta;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.notification.FinanceNotifier;
import com.pousadamarealta.notification.GovernanceNotifier;
import com.pousadamarealta.notification.NotificationService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.route.MessageRoutes;
import com.pousadamarealta.route.ReportRoutes;
import com.pousadamarealta.route.ReservationRoutes;
import com.pousadamarealta.route.RoomRoutes;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Database configuration not found. Set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
            System.exit(1);
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database.initialize(dbUrl, dbUser, dbPassword);

        ReservationRepository reservationRepository = new ReservationRepository();
        RoomRepository roomRepository = new RoomRepository();
        MessageRepository messageRepository = new MessageRepository();

        NotificationService notificationService = new NotificationService();
        notificationService.register(new GovernanceNotifier(messageRepository));
        notificationService.register(new FinanceNotifier(messageRepository));

        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageRepository, notificationService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        Javalin app = Javalin.create()
                .start(port);

        RoomRoutes.register(app, roomRepository);
        ReservationRoutes.register(app, reservationService);
        MessageRoutes.register(app, messageRepository);
        ReportRoutes.register(app, reportService);

        System.out.println("Pousada Maré Alta - Reservations System started on port " + port);
    }
}
