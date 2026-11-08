package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.*;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.domain.events.EventPublisher;
import com.pousadamarealta.domain.events.ReservationCancelled;
import com.pousadamarealta.domain.events.ReservationConfirmed;
import com.pousadamarealta.infrastructure.DatabaseInitializer;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.listeners.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Application {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new JdbcRoomRepository(dataSource);
        ReservationRepository reservationRepository = new JdbcReservationRepository(dataSource);
        MessageRepository messageRepository = new JdbcMessageRepository(dataSource);

        EventPublisher eventPublisher = new EventPublisher();
        NotificationService notificationService = new NotificationService(messageRepository);

        eventPublisher.subscribe(ReservationConfirmed.class, new GuestConfirmationListener(notificationService, roomRepository));
        eventPublisher.subscribe(ReservationConfirmed.class, new HousekeepingConfirmationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmed.class, new FinanceConfirmationListener(notificationService));

        eventPublisher.subscribe(ReservationCancelled.class, new HousekeepingCancellationListener(notificationService));
        eventPublisher.subscribe(ReservationCancelled.class, new FinanceCancellationListener(notificationService));

        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository, eventPublisher);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService, roomRepository);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        int port = getPort();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", roomController::listRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::listMessages);

        app.get("/reports/occupancy", reportController::occupancyReport);
        app.get("/reports/revenue", reportController::revenueReport);

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        });

        app.start(port);

        System.out.println("Server started on port " + port);
    }

    private static DataSource createDataSource() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new RuntimeException("Missing database configuration");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }
}
