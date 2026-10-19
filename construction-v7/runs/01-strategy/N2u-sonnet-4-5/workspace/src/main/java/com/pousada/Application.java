package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.controller.MessageController;
import com.pousada.api.controller.ReportController;
import com.pousada.api.controller.ReservationController;
import com.pousada.api.controller.RoomController;
import com.pousada.api.dto.ErrorResponse;
import com.pousada.service.MessageService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.format.DateTimeParseException;

public class Application {

    public static void main(String[] args) {
        int port = getPort();

        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        ReservationService reservationService = new ReservationService(roomService, messageService);
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

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) ->
            sendError(ctx, 404, "ROOM_NOT_FOUND", e.getMessage()));

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) ->
            sendError(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage()));

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) ->
            sendError(ctx, 400, "INVALID_REQUEST", e.getMessage()));

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) ->
            sendError(ctx, 409, "ROOM_UNAVAILABLE", e.getMessage()));

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) ->
            sendError(ctx, 422, "CAPACITY_EXCEEDED", e.getMessage()));

        app.exception(IllegalStateException.class, (e, ctx) ->
            sendError(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage()));

        app.exception(IllegalArgumentException.class, (e, ctx) ->
            sendError(ctx, 400, "INVALID_REQUEST", e.getMessage()));

        app.exception(DateTimeParseException.class, (e, ctx) ->
            sendError(ctx, 400, "INVALID_REQUEST", "Invalid date format"));

        app.exception(Exception.class, (e, ctx) -> {
            if (e.getMessage() != null && e.getMessage().contains("Required body")) {
                sendError(ctx, 400, "INVALID_REQUEST", "Request body is required");
            } else if (e.getMessage() != null && e.getMessage().contains("JSON")) {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid JSON format");
            } else {
                e.printStackTrace();
                sendError(ctx, 500, "INTERNAL_ERROR", "Internal server error");
            }
        });

        app.start(port);
        System.out.println("Server started on port " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }

    private static void sendError(Context ctx, int status, String errorCode, String message) {
        ctx.status(status).json(new ErrorResponse(errorCode, message));
    }
}
