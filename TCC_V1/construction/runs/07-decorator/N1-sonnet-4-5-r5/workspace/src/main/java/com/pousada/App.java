package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.*;
import com.pousada.config.Database;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.MessageService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import com.pousada.service.messaging.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = getEnv("DB_USER", "postgres");
        String dbPassword = getEnv("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(getEnv("PORT", "7070"));
        boolean testMode = Boolean.parseBoolean(getEnv("HOTEL_TEST_MODE", "false"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        MessageProcessor messageProcessor = createMessageProcessor(testMode);

        MessageService messageService = new MessageService(messageRepository, messageProcessor);
        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
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
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        Runtime.getRuntime().addShutdownHook(new Thread(database::close));
    }

    private static MessageProcessor createMessageProcessor(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageProcessor(rules);
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
