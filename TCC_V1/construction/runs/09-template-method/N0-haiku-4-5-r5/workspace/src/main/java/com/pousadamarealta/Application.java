package com.pousadamarealta;

import io.javalin.Javalin;
import com.pousadamarealta.db.Database;
import com.pousadamarealta.routes.RoomRoutes;
import com.pousadamarealta.routes.ReservationRoutes;
import com.pousadamarealta.routes.MessageRoutes;
import com.pousadamarealta.routes.ReportRoutes;

public class Application {
    public static void main(String[] args) {
        Database.init();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create()
            .start(port);

        app.get("/rooms", new RoomRoutes()::listRooms);

        app.post("/reservations", new ReservationRoutes()::createReservation);
        app.get("/reservations/{id}", new ReservationRoutes()::getReservation);
        app.post("/reservations/{id}/confirmation", new ReservationRoutes()::confirmReservation);
        app.post("/reservations/{id}/cancellation", new ReservationRoutes()::cancelReservation);
        app.post("/reservations/{id}/check-in", new ReservationRoutes()::checkInReservation);
        app.post("/reservations/{id}/check-out", new ReservationRoutes()::checkOutReservation);
        app.get("/reservations/{id}/document", new ReservationRoutes()::getDocument);

        app.get("/messages", new MessageRoutes()::listMessages);

        app.get("/reports/occupancy", new ReportRoutes()::occupancy);
        app.get("/reports/revenue", new ReportRoutes()::revenue);
        app.get("/reports/tourism-tax", new ReportRoutes()::tourismTax);
        app.get("/reports/rate-plans", new ReportRoutes()::ratePlans);

        app.error(404, ctx -> ctx.json(new ErrorResponse("NOT_FOUND", "Not found")));
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
