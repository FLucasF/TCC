package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.controller.MessageController;
import com.pousadamarealta.api.controller.ReportController;
import com.pousadamarealta.api.controller.ReservationController;
import com.pousadamarealta.api.controller.RoomController;
import com.pousadamarealta.domain.listener.FinanceNotificationListener;
import com.pousadamarealta.domain.listener.GuestNotificationListener;
import com.pousadamarealta.domain.listener.HousekeepingNotificationListener;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.domain.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository);
        EventBus eventBus = new EventBus();

        eventBus.subscribe(new GuestNotificationListener(messageService));
        eventBus.subscribe(new HousekeepingNotificationListener(messageService));
        eventBus.subscribe(new FinanceNotificationListener(messageService));

        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, pricingService, eventBus
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, roomService
        );
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", roomController::getRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
    }
}
