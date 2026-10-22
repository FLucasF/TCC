package com.pousadamareal;

import com.pousadamareal.config.DatabaseConfig;
import com.pousadamareal.handlers.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        DatabaseConfig.init();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        var app = Javalin.create().start(port);

        app.get("/rooms", RoomHandler::getRooms);

        app.post("/reservations", ReservationHandler::createReservation);
        app.get("/reservations/{id}", ReservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", ReservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", ReservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", ReservationHandler::checkOut);
        app.get("/reservations/{id}/document", ReservationHandler::getDocument);

        app.get("/messages", MessageHandler::getMessages);

        app.get("/reports/occupancy", ReportHandler::getOccupancyReport);
        app.get("/reports/revenue", ReportHandler::getRevenueReport);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
            ctx.status(500);
        });
    }

    public record ErrorResponse(String error, String message) {}
}
