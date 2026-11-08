package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.ErrorResponse;
import com.pousadamarealta.api.controller.MessageController;
import com.pousadamarealta.api.controller.ReportController;
import com.pousadamarealta.api.controller.ReservationController;
import com.pousadamarealta.api.controller.RoomController;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.listeners.FinanceNotificationListener;
import com.pousadamarealta.domain.event.listeners.GuestNotificationListener;
import com.pousadamarealta.domain.event.listeners.HousekeepingNotificationListener;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.service.ReportService;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import java.sql.Connection;
import java.time.format.DateTimeParseException;

public class Main {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portEnv = System.getenv("PORT");
        int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);
        Connection connection = database.getConnection();

        RoomRepository roomRepository = new RoomRepository(connection);
        ReservationRepository reservationRepository = new ReservationRepository(connection);
        MessageRepository messageRepository = new MessageRepository(connection);

        roomRepository.initialize();
        reservationRepository.initialize();
        messageRepository.initialize();

        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        EventPublisher eventPublisher = new EventPublisher();
        eventPublisher.registerConfirmedListener(new GuestNotificationListener(messageService));
        eventPublisher.registerConfirmedListener(new HousekeepingNotificationListener(messageService));
        eventPublisher.registerConfirmedListener(new FinanceNotificationListener(messageService));
        eventPublisher.registerCancelledListener(new HousekeepingNotificationListener(messageService));
        eventPublisher.registerCancelledListener(new FinanceNotificationListener(messageService));

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, eventPublisher
        );

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

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

        app.exception(RoomService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(DateTimeParseException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        });

        app.exception(ReservationService.ValidationException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.stop();
                database.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
