package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.MessageController;
import com.pousadamarealta.api.ReportController;
import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.api.RoomController;
import com.pousadamarealta.messaging.*;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        int port = getPort();
        boolean testMode = isTestMode();

        RoomService roomService = new RoomService();
        ReservationService reservationService = new ReservationService(roomService);

        MessageProcessor messageProcessor = buildMessageProcessor(testMode);
        MessageService messageService = new MessageService(messageProcessor);

        ReportService reportService = new ReportService(reservationService, roomService);

        Javalin app = createApp(roomService, reservationService, messageService, reportService);
        app.start(port);
    }

    private static Javalin createApp(RoomService roomService, ReservationService reservationService,
                                     MessageService messageService, ReportService reportService) {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, messageService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

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

        return app;
    }

    private static MessageProcessor buildMessageProcessor(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();

        if (testMode) {
            rules.add(new TestModeRule("testes@pousadamarealta.com.br"));
        }

        rules.add(new EmailValidationRule());

        if (!testMode) {
            rules.add(new ArchiveCopyRule("arquivo@pousadamarealta.com.br", 1000000));
        }

        return new MessageProcessor(rules);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }

    private static boolean isTestMode() {
        String testMode = System.getenv("HOTEL_TEST_MODE");
        return "true".equalsIgnoreCase(testMode);
    }
}
