package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.event.EventPublisher;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.event.listeners.*;
import com.pousadamarealta.exception.AppException;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portEnv = System.getenv("PORT");
        int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PricingService pricingService = new PricingService();
        NotificationService notificationService = new NotificationService(messageRepository);
        RoomService roomService = new RoomService(roomRepository);

        EventPublisher eventPublisher = new EventPublisher();

        eventPublisher.subscribe(ReservationConfirmedEvent.class,
                new GuestNotificationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
                new HousekeepingNotificationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
                new FinanceNotificationListener(notificationService));

        eventPublisher.subscribe(ReservationCancelledEvent.class,
                new HousekeepingCancellationListener(notificationService));
        eventPublisher.subscribe(ReservationCancelledEvent.class,
                new FinanceCancellationListener(notificationService));

        ReservationService reservationService = new ReservationService(
                reservationRepository, roomService, pricingService, eventPublisher);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, objectMapper);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(port);

        app.get("/rooms", roomController::getRooms);

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

        app.exception(AppException.class, Main::handleAppException);
        app.exception(NumberFormatException.class, (e, ctx) -> {
            handleAppException(AppException.reservationNotFound(), ctx);
        });
    }

    private static void handleAppException(AppException e, Context ctx) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getErrorCode());
        error.put("message", e.getMessage());
        ctx.status(e.getStatusCode()).json(error);
    }
}
