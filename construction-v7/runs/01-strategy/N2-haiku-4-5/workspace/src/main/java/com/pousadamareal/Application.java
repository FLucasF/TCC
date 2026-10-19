package com.pousadamareal;

import com.pousadamareal.api.*;
import com.pousadamareal.service.DataStore;
import com.pousadamareal.service.ReportService;
import com.pousadamareal.service.ReservationService;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        DataStore dataStore = new DataStore();
        ReservationService reservationService = new ReservationService(dataStore);
        ReportService reportService = new ReportService(dataStore);

        RoomController roomController = new RoomController(dataStore);
        ReservationController reservationController = new ReservationController(reservationService, dataStore);
        MessageController messageController = new MessageController(dataStore);
        ReportController reportController = new ReportController(reportService);

        int port = getPort();
        Javalin app = Javalin.create().start(port);

        app.get("/rooms", roomController::getRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getReceipt);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }
}
