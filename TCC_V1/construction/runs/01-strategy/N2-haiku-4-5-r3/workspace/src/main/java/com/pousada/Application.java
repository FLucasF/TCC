package com.pousada;

import com.pousada.database.Database;
import com.pousada.api.*;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) throws Exception {
        Database.init();

        Javalin app = Javalin.create();

        app.get("/rooms", RoomHandler::listRooms);
        app.post("/reservations", ReservationHandler::create);
        app.get("/reservations/{id}", ReservationHandler::get);
        app.post("/reservations/{id}/confirmation", ReservationHandler::confirm);
        app.post("/reservations/{id}/cancellation", ReservationHandler::cancel);
        app.post("/reservations/{id}/check-in", ReservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", ReservationHandler::checkOut);
        app.get("/reservations/{id}/document", ReservationHandler::document);
        app.get("/messages", MessageHandler::list);
        app.get("/reports/occupancy", ReportHandler::occupancy);
        app.get("/reports/revenue", ReportHandler::revenue);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        app.start(port);
    }
}
