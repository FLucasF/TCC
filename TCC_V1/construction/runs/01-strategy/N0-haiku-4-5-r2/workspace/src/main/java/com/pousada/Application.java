package com.pousada;

import com.pousada.database.Database;
import com.pousada.handler.*;
import com.pousada.service.RoomService;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Error: DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
            System.exit(1);
        }

        try {
            Database.initialize(dbUrl, dbUser, dbPassword);
            RoomService.loadRooms();
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        Javalin app = Javalin.create().start(port);

        // Room endpoints
        app.get("/rooms", RoomHandler::listRooms);

        // Reservation endpoints
        app.post("/reservations", ReservationHandler::createReservation);
        app.get("/reservations/{id}", ReservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", ReservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", ReservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", ReservationHandler::checkOut);
        app.get("/reservations/{id}/document", ReservationHandler::getDocument);

        // Message endpoints
        app.get("/messages", MessageHandler::listMessages);

        // Report endpoints
        app.get("/reports/occupancy", ReportHandler::occupancyReport);
        app.get("/reports/revenue", ReportHandler::revenueReport);

        System.out.println("Pousada Maré Alta reservation system started on port " + port);
    }
}
