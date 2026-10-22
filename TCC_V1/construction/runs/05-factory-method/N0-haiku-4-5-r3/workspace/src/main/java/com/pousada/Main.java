package com.pousada;

import com.pousada.config.DatabaseConfig;
import com.pousada.handler.OccupancyReportHandler;
import com.pousada.handler.ReservationHandler;
import com.pousada.handler.RevenueReportHandler;
import com.pousada.handler.RoomHandler;
import com.pousada.handler.MessageHandler;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Initialize database
        DatabaseConfig.initialize();

        // Get port from environment or use default
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        // Create Javalin app
        var app = Javalin.create()
                .start(port);

        logger.info("Starting Pousada Maré Alta Reservations System on port {}", port);

        // Register routes
        app.get("/rooms", RoomHandler::getRooms);

        app.post("/reservations", ReservationHandler::createReservation);
        app.get("/reservations/{id}", ReservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", ReservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", ReservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", ReservationHandler::checkOut);
        app.get("/reservations/{id}/document", ReservationHandler::getDocument);

        app.get("/messages", MessageHandler::getMessages);

        app.get("/reports/occupancy", OccupancyReportHandler::getOccupancyReport);
        app.get("/reports/revenue", RevenueReportHandler::getRevenueReport);
    }
}
