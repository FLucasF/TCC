package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.controller.*;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.database.*;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration required: DB_URL, DB_USER, DB_PASSWORD");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        DatabaseManager dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);

        RoomDAO roomDAO = new RoomDAO(dbManager);
        ReservationDAO reservationDAO = new ReservationDAO(dbManager);
        MessageDAO messageDAO = new MessageDAO(dbManager);

        RoomService roomService = new RoomService(roomDAO);
        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageDAO, testMode);
        ReservationService reservationService = new ReservationService(
            reservationDAO, roomService, pricingService, messageService);
        ReportService reportService = new ReportService(roomService, reservationDAO);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new io.javalin.json.JsonMapper() {
                @Override
                public String toJsonString(Object obj, java.lang.reflect.Type type) {
                    try {
                        return objectMapper.writeValueAsString(obj);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public <T> T fromJsonString(String json, java.lang.reflect.Type targetType) {
                    try {
                        return objectMapper.readValue(json, objectMapper.constructType(targetType));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });

        app.get("/rooms", roomController::getAllRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAllMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid number format"));
        });

        app.exception(com.fasterxml.jackson.core.JsonProcessingException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON"));
        });

        app.start(port);

        System.out.println("Server started on port " + port);
    }
}
