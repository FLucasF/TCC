package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.controller.MessageController;
import com.pousada.controller.ReportController;
import com.pousada.controller.ReservationController;
import com.pousada.controller.RoomController;
import com.pousada.dto.ErrorResponse;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.NotificationService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnprocessableContentResponse;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        int port = getPort();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        NotificationService notificationService = new NotificationService();

        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, notificationService
        );
        ReportService reportService = new ReportService(
                reservationRepository, roomRepository
        );

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
                reservationService, roomRepository
        );
        ReportController reportController = new ReportController(reportService);
        MessageController messageController = new MessageController(notificationService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            String errorCode = e.getMessage();

            switch (errorCode) {
                case "ROOM_NOT_FOUND":
                case "RESERVATION_NOT_FOUND":
                    ctx.status(404).json(new ErrorResponse(errorCode, getErrorMessage(errorCode)));
                    break;
                case "ROOM_UNAVAILABLE":
                case "OPERATION_NOT_ALLOWED":
                    ctx.status(409).json(new ErrorResponse(errorCode, getErrorMessage(errorCode)));
                    break;
                case "CAPACITY_EXCEEDED":
                    ctx.status(422).json(new ErrorResponse(errorCode, getErrorMessage(errorCode)));
                    break;
                case "INVALID_REQUEST":
                default:
                    ctx.status(400).json(new ErrorResponse(errorCode, getErrorMessage(errorCode)));
                    break;
            }
        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            String errorCode = e.getMessage();
            ctx.status(409).json(new ErrorResponse(errorCode, getErrorMessage(errorCode)));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        });

        System.out.println("Pousada Maré Alta - Sistema rodando na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }

    private static String getErrorMessage(String errorCode) {
        return switch (errorCode) {
            case "ROOM_NOT_FOUND" -> "Room not found";
            case "RESERVATION_NOT_FOUND" -> "Reservation not found";
            case "ROOM_UNAVAILABLE" -> "Room not available for selected dates";
            case "OPERATION_NOT_ALLOWED" -> "Operation not allowed in current state";
            case "CAPACITY_EXCEEDED" -> "Number of guests exceeds room capacity";
            case "INVALID_REQUEST" -> "Invalid request";
            default -> "Error processing request";
        };
    }
}
