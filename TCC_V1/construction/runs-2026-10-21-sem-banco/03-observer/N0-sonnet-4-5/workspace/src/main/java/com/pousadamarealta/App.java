package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.event.*;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        MessageService messageService = new MessageService();
        RoomService roomService = new RoomService();
        PricingService pricingService = new PricingService();
        EventService eventService = new EventService();
        ReservationService reservationService = new ReservationService(roomService, pricingService, eventService);
        ReportService reportService = new ReportService(reservationService, roomService);

        registerEventHandlers(eventService, messageService, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", roomController::getAll);

        app.post("/reservations", reservationController::create);
        app.get("/reservations/{id}", reservationController::getById);
        app.post("/reservations/{id}/confirmation", reservationController::confirm);
        app.post("/reservations/{id}/cancellation", reservationController::cancel);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAll);

        app.get("/reports/occupancy", reportController::getOccupancy);
        app.get("/reports/revenue", reportController::getRevenue);
    }

    private static void registerEventHandlers(EventService eventService, MessageService messageService, RoomService roomService) {
        eventService.register(ReservationConfirmedEvent.class, new GuestNotificationHandler(messageService, roomService));
        eventService.register(ReservationConfirmedEvent.class, new HousekeepingNotificationHandler(messageService));
        eventService.register(ReservationConfirmedEvent.class, new FinanceConfirmationHandler(messageService));

        eventService.register(ReservationCancelledEvent.class, new HousekeepingCancellationHandler(messageService));
        eventService.register(ReservationCancelledEvent.class, new FinanceCancellationHandler(messageService));
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }
}
