package com.pousadamarealta;

import com.pousadamarealta.api.*;
import com.pousadamarealta.events.ReservationEventDispatcher;
import com.pousadamarealta.listeners.*;
import com.pousadamarealta.services.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
            System.exit(1);
        }

        Database database = new Database(dbUrl, dbUser, dbPassword);
        ReservationEventDispatcher eventDispatcher = new ReservationEventDispatcher();

        MessageService messageService = new MessageService(database);
        DocumentService documentService = new DocumentService(database);
        ReservationService reservationService = new ReservationService(database, eventDispatcher);
        ReportService reportService = new ReportService(database);

        eventDispatcher.addListener(new GuestNotificationListener(messageService, documentService));
        eventDispatcher.addListener(new GovernanceNotificationListener(messageService));
        eventDispatcher.addListener(new FinanceNotificationListener(messageService));

        Javalin app = Javalin.create()
            .start(port);

        RoomsController.register(app, database);
        ReservationsController.register(app, reservationService, documentService, database);
        MessagesController.register(app, messageService);
        ReportsController.register(app, reportService);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }
}
