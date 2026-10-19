package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.handler.*;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.HashMap;
import java.util.Map;

public class Application {

    public static void main(String[] args) {
        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        ReservationService reservationService = new ReservationService(roomService, messageService);
        ReportService reportService = new ReportService(reservationService, roomService);

        RoomHandler roomHandler = new RoomHandler(roomService);
        ReservationHandler reservationHandler = new ReservationHandler(reservationService);
        MessageHandler messageHandler = new MessageHandler(messageService);
        ReportHandler reportHandler = new ReportHandler(reportService);

        int port = getPort();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", roomHandler::getRooms);
        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);
        app.get("/messages", messageHandler::getMessages);
        app.get("/reports/occupancy", reportHandler::getOccupancyReport);
        app.get("/reports/revenue", reportHandler::getRevenueReport);

        app.exception(ServiceException.class, (e, ctx) -> {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getErrorCode());
            error.put("message", e.getMessage());
            ctx.status(e.getStatusCode()).json(error);
        });

        app.exception(NumberFormatException.class, (e, ctx) -> {
            Map<String, String> error = new HashMap<>();
            error.put("error", "INVALID_REQUEST");
            error.put("message", "Invalid number format");
            ctx.status(400).json(error);
        });

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Server running on port " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable, using default 7070");
            }
        }
        return 7070;
    }
}
