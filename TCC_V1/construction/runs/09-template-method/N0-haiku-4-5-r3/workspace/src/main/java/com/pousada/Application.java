package com.pousada;

import com.pousada.database.Database;
import com.pousada.routes.ReservationRoutes;
import com.pousada.routes.RoomRoutes;
import com.pousada.routes.MessageRoutes;
import com.pousada.routes.ReportRoutes;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database db = new Database(dbUrl, dbUser, dbPassword);
        db.init();

        var app = Javalin.create().start(port);

        app.get("/rooms", new RoomRoutes(db)::getRooms);

        var resRoutes = new ReservationRoutes(db);
        app.post("/reservations", resRoutes::createReservation);
        app.get("/reservations/{id}", resRoutes::getReservation);
        app.post("/reservations/{id}/confirmation", resRoutes::confirmReservation);
        app.post("/reservations/{id}/cancellation", resRoutes::cancelReservation);
        app.post("/reservations/{id}/check-in", resRoutes::checkIn);
        app.post("/reservations/{id}/check-out", resRoutes::checkOut);
        app.get("/reservations/{id}/document", resRoutes::getDocument);

        var msgRoutes = new MessageRoutes(db);
        app.get("/messages", msgRoutes::getMessages);

        var reportRoutes = new ReportRoutes(db);
        app.get("/reports/occupancy", reportRoutes::getOccupancy);
        app.get("/reports/revenue", reportRoutes::getRevenue);
        app.get("/reports/tourism-tax", reportRoutes::getTouristTax);
        app.get("/reports/rate-plans", reportRoutes::getRatePlans);
    }
}
