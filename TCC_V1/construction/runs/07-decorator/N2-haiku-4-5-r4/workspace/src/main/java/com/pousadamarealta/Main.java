package com.pousadamarealta;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.handler.*;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            logger.error("Missing database configuration: DB_URL, DB_USER, DB_PASSWORD");
            System.exit(1);
        }

        Database database = new Database(dbUrl, dbUser, dbPassword);
        MessageService messageService = new MessageService(database);
        ReservationService reservationService = new ReservationService(database, messageService);
        ReportService reportService = new ReportService(database, reservationService);

        RoomHandler roomHandler = new RoomHandler(reservationService);
        ReservationHandler reservationHandler = new ReservationHandler(reservationService);
        MessageHandler messageHandler = new MessageHandler(messageService);
        ReportHandler reportHandler = new ReportHandler(reportService);

        Javalin app = Javalin.create()
                .start(port);

        logger.info("Server started on port {}", port);

        // Room endpoints
        app.get("/rooms", roomHandler::listRooms);

        // Reservation endpoints
        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);

        // Message endpoints
        app.get("/messages", messageHandler::listMessages);

        // Report endpoints
        app.get("/reports/occupancy", reportHandler::getOccupancyReport);
        app.get("/reports/revenue", reportHandler::getRevenueReport);

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            app.stop();
            database.close();
        }));
    }
}
