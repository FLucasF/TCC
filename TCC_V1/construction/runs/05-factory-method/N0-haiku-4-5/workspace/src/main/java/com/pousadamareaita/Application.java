package com.pousadamareaita;

import io.javalin.Javalin;
import com.pousadamareaita.database.Database;
import com.pousadamareaita.api.RoomHandler;
import com.pousadamareaita.api.ReservationHandler;
import com.pousadamareaita.api.MessageHandler;
import com.pousadamareaita.api.ReportHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Application {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
        }

        Database db = new Database(dbUrl, dbUser, dbPassword);
        db.initialize();

        Javalin app = Javalin.create().start(port);

        ObjectMapper mapper = new ObjectMapper();

        RoomHandler roomHandler = new RoomHandler(db);
        ReservationHandler reservationHandler = new ReservationHandler(db);
        MessageHandler messageHandler = new MessageHandler(db);
        ReportHandler reportHandler = new ReportHandler(db);

        app.get("/rooms", roomHandler::getRooms);
        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);
        app.get("/messages", messageHandler::getMessages);
        app.get("/reports/occupancy", reportHandler::getOccupancyReport);
        app.get("/reports/revenue", reportHandler::getRevenueReport);

        System.out.println("Pousada Maré Alta - Sistema de Reservas iniciado na porta " + port);
    }
}
