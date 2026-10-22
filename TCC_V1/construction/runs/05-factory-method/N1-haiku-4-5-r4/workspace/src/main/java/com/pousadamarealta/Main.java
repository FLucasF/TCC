package com.pousadamarealta;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        var database = new Database();
        database.initialize();

        var app = Javalin.create()
            .start(port);

        var roomController = new RoomController(database);
        var reservationController = new ReservationController(database);
        var messageController = new MessageController(database);
        var reportController = new ReportController(database);

        roomController.register(app);
        reservationController.register(app);
        messageController.register(app);
        reportController.register(app);

        logger.info("Pousada Maré Alta Reservation System started on port {}", port);
    }
}
