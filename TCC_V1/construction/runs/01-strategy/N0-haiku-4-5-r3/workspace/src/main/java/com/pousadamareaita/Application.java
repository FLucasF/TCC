package com.pousadamareaita;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("DB_URL, DB_USER, and DB_PASSWORD environment variables must be set");
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        Javalin app = Javalin.create()
                .get("/rooms", ctx -> RoomHandler.listRooms(ctx))
                .post("/reservations", ctx -> ReservationHandler.createReservation(ctx))
                .get("/reservations/{id}", ctx -> ReservationHandler.getReservation(ctx))
                .post("/reservations/{id}/confirmation", ctx -> ReservationHandler.confirmReservation(ctx))
                .post("/reservations/{id}/cancellation", ctx -> ReservationHandler.cancelReservation(ctx))
                .post("/reservations/{id}/check-in", ctx -> ReservationHandler.checkIn(ctx))
                .post("/reservations/{id}/check-out", ctx -> ReservationHandler.checkOut(ctx))
                .get("/reservations/{id}/document", ctx -> ReservationHandler.getDocument(ctx))
                .get("/messages", ctx -> MessageHandler.listMessages(ctx))
                .get("/reports/occupancy", ctx -> ReportHandler.occupancy(ctx))
                .get("/reports/revenue", ctx -> ReportHandler.revenue(ctx))
                .start(port);

        logger.info("Server started on port {}", port);
    }
}
