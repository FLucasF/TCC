package com.pousada;

import com.pousada.infrastructure.Database;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        Database.initialize();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create();

        // Rooms
        app.get("/rooms", new RoomsHandler()::list);

        // Reservations
        app.post("/reservations", new ReservationsHandler()::create);
        app.get("/reservations/{id}", new ReservationsHandler()::get);
        app.post("/reservations/{id}/confirmation", new ReservationsHandler()::confirm);
        app.post("/reservations/{id}/cancellation", new ReservationsHandler()::cancel);
        app.post("/reservations/{id}/check-in", new ReservationsHandler()::checkIn);
        app.post("/reservations/{id}/check-out", new ReservationsHandler()::checkOut);
        app.get("/reservations/{id}/document", new ReservationsHandler()::getDocument);

        // Messages
        app.get("/messages", new MessagesHandler()::list);

        // Reports
        app.get("/reports/occupancy", new ReportsHandler()::occupancy);
        app.get("/reports/revenue", new ReportsHandler()::revenue);
        app.get("/reports/tourism-tax", new ReportsHandler()::tourismTax);
        app.get("/reports/rate-plans", new ReportsHandler()::ratePlans);

        app.start(port);
    }
}
