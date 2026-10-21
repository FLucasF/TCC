package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.controller.MessageController;
import com.pousada.controller.ReportController;
import com.pousada.controller.ReservationController;
import com.pousada.controller.RoomController;
import com.pousada.exception.BusinessException;
import com.pousada.service.MessageService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.Map;

public class App {

    public static void main(String[] args) {
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

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.exception(BusinessException.class, (e, ctx) -> {
            ctx.status(e.getHttpStatus());
            ctx.json(Map.of(
                "error", e.getErrorCode(),
                "message", e.getMessage()
            ));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An internal error occurred"
            ));
        });

        app.get("/rooms", roomController::getAllRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAllMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        System.out.println("Pousada Maré Alta server started on port " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable, using default 7070");
            }
        }
        return 7070;
    }
}
