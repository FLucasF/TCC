package com.pousadamareaalta;

import com.pousadamareaalta.handlers.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String port = System.getenv("PORT");

        int portNumber = port != null ? Integer.parseInt(port) : 7070;

        Database.init(dbUrl, dbUser, dbPassword);

        Javalin app = Javalin.create()
                .get("/rooms", new RoomHandler()::list)
                .post("/reservations", new ReservationHandler()::create)
                .get("/reservations/:id", new ReservationHandler()::get)
                .post("/reservations/:id/confirmation", new ReservationHandler()::confirm)
                .post("/reservations/:id/cancellation", new ReservationHandler()::cancel)
                .post("/reservations/:id/check-in", new ReservationHandler()::checkIn)
                .post("/reservations/:id/check-out", new ReservationHandler()::checkOut)
                .get("/reservations/:id/document", new ReservationHandler()::getDocument)
                .get("/messages", new MessageHandler()::list)
                .get("/reports/occupancy", new ReportHandler()::occupancy)
                .get("/reports/revenue", new ReportHandler()::revenue)
                .start(portNumber);
    }
}
