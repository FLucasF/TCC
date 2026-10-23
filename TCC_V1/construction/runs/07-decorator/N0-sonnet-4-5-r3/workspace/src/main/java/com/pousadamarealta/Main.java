package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.*;
import com.pousadamarealta.database.DatabaseInitializer;
import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.service.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        DataSource dataSource = createDataSource(dbUrl, dbUser, dbPassword);
        DatabaseInitializer.initialize(dataSource);

        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        RoomService roomService = new RoomService();
        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository, testMode);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, pricingService, messageService);
        ReportService reportService = new ReportService(reservationService, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
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

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(
                new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        });
    }

    private static DataSource createDataSource(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }
}
