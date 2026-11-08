package com.marealta.api;

import io.javalin.Javalin;
import com.marealta.api.handlers.*;

public class Routes {
    public static void register(Javalin app) {
        app.get("/rooms", RoomHandler::list);

        app.post("/reservations", ReservationHandler::create);
        app.get("/reservations/{id}", ReservationHandler::get);
        app.post("/reservations/{id}/confirmation", ReservationHandler::confirm);
        app.post("/reservations/{id}/cancellation", ReservationHandler::cancel);
        app.post("/reservations/{id}/check-in", ReservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", ReservationHandler::checkOut);
        app.get("/reservations/{id}/document", ReservationHandler::getDocument);

        app.get("/messages", MessageHandler::list);

        app.get("/reports/occupancy", ReportHandler::occupancy);
        app.get("/reports/revenue", ReportHandler::revenue);

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
        });
    }
}
