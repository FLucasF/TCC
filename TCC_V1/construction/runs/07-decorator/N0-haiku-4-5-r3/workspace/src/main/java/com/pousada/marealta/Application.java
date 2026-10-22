package com.pousada.marealta;

import io.javalin.Javalin;
import com.pousada.marealta.db.Database;
import com.pousada.marealta.routes.RoomRoutes;
import com.pousada.marealta.routes.ReservationRoutes;
import com.pousada.marealta.routes.MessageRoutes;
import com.pousada.marealta.routes.ReportRoutes;

public class Application {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database.initialize();

        Javalin app = Javalin.create();

        app.get("/rooms", RoomRoutes::getAllRooms);
        app.post("/reservations", ReservationRoutes::createReservation);
        app.get("/reservations/{id}", ReservationRoutes::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationRoutes::confirmReservation);
        app.post("/reservations/{id}/cancellation", ReservationRoutes::cancelReservation);
        app.post("/reservations/{id}/check-in", ReservationRoutes::checkIn);
        app.post("/reservations/{id}/check-out", ReservationRoutes::checkOut);
        app.get("/reservations/{id}/document", ReservationRoutes::getDocument);

        app.get("/messages", MessageRoutes::getMessages);

        app.get("/reports/occupancy", ReportRoutes::getOccupancyReport);
        app.get("/reports/revenue", ReportRoutes::getRevenueReport);

        app.start(port);
    }
}
