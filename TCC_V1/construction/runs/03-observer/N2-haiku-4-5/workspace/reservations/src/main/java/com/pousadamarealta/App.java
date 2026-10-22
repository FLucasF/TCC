package com.pousadamarealta;

import com.pousadamarealta.api.ApiErrorHandler;
import com.pousadamarealta.api.ReportsHandler;
import com.pousadamarealta.api.ReservationsHandler;
import com.pousadamarealta.api.RoomsHandler;
import com.pousadamarealta.api.MessagesHandler;
import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.ReservationEventDispatcher;
import com.pousadamarealta.service.FinanceNotificationListener;
import com.pousadamarealta.service.GovernanceNotificationListener;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database environment variables not set");
        }

        DatabaseManager.initialize(dbUrl, dbUser, dbPassword);

        try {
            RoomDAO.getInstance().initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }

        ReservationEventDispatcher.getInstance().addListener(new GovernanceNotificationListener());
        ReservationEventDispatcher.getInstance().addListener(new FinanceNotificationListener());

        Javalin app = Javalin.create()
                .start(port);

        app.get("/rooms", RoomsHandler::getRooms);

        app.post("/reservations", ReservationsHandler::createReservation);
        app.get("/reservations/{id}", ReservationsHandler::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationsHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", ReservationsHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", ReservationsHandler::checkIn);
        app.post("/reservations/{id}/check-out", ReservationsHandler::checkOut);
        app.get("/reservations/{id}/document", ReservationsHandler::getDocument);

        app.get("/messages", MessagesHandler::getMessages);

        app.get("/reports/occupancy", ReportsHandler::getOccupancyReport);
        app.get("/reports/revenue", ReportsHandler::getRevenueReport);

        app.exception(Exception.class, ApiErrorHandler::handleException);

        System.out.println("Server started on port " + port);
    }
}
