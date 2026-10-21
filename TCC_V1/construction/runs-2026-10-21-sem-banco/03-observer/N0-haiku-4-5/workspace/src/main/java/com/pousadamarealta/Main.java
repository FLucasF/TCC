package com.pousadamarealta;

import com.pousadamarealta.api.MessageController;
import com.pousadamarealta.api.ReportController;
import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.api.RoomController;
import com.pousadamarealta.notification.EventDispatcher;
import com.pousadamarealta.notification.FinancialCancellationNotifier;
import com.pousadamarealta.notification.FinancialConfirmationNotifier;
import com.pousadamarealta.notification.GovernanceCancellationNotifier;
import com.pousadamarealta.notification.GovernanceConfirmationNotifier;
import com.pousadamarealta.notification.GuestConfirmationNotifier;
import com.pousadamarealta.notification.MessageStore;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) {
        int port = getPort();

        var roomService = new RoomService();
        var pricingService = new PricingService();
        var messageStore = new MessageStore();
        var messageIdCounter = new AtomicLong(0);
        var eventDispatcher = new EventDispatcher();
        var reservationService = new ReservationService(roomService, pricingService, eventDispatcher);
        var reportService = new ReportService(reservationService, roomService);

        eventDispatcher.subscribe(new GuestConfirmationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new GovernanceConfirmationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new FinancialConfirmationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new GovernanceCancellationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new FinancialCancellationNotifier(messageStore, messageIdCounter));

        var roomController = new RoomController(roomService);
        var reservationController = new ReservationController(reservationService);
        var messageController = new MessageController(messageStore);
        var reportController = new ReportController(reportService);

        var app = Javalin.create();

        app.get("/rooms", roomController::getAllRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.start(port);
        System.out.println("Servidor iniciado na porta " + port);
    }

    private static int getPort() {
        String portStr = System.getenv("PORT");
        if (portStr == null || portStr.isEmpty()) {
            return 7070;
        }
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return 7070;
        }
    }
}
