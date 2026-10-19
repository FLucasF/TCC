package br.com.mareaalta;

import br.com.mareaalta.api.*;
import br.com.mareaalta.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        ReservationService reservationService = new ReservationService(roomService, messageService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reservationService);

        int port = getPort();

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
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        setupExceptionHandlers(app);
    }

    private static void setupExceptionHandlers(Javalin app) {
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            sendError(ctx, 400, "INVALID_REQUEST", e.getMessage());
        });

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            sendError(ctx, 400, "INVALID_REQUEST", e.getMessage());
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            sendError(ctx, 404, "ROOM_NOT_FOUND", e.getMessage());
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            sendError(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            sendError(ctx, 409, "ROOM_UNAVAILABLE", e.getMessage());
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            sendError(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            sendError(ctx, 422, "CAPACITY_EXCEEDED", e.getMessage());
        });

        app.exception(com.fasterxml.jackson.databind.JsonMappingException.class, (e, ctx) -> {
            sendError(ctx, 400, "INVALID_REQUEST", "Invalid JSON format");
        });

        app.exception(com.fasterxml.jackson.core.JsonParseException.class, (e, ctx) -> {
            sendError(ctx, 400, "INVALID_REQUEST", "Invalid JSON format");
        });
    }

    private static void sendError(Context ctx, int status, String errorCode, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", errorCode);
        error.put("message", message);
        ctx.status(status).json(error);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable: " + portEnv);
            }
        }
        return 7070;
    }
}
