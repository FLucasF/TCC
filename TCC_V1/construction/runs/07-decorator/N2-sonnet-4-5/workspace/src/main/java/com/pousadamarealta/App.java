package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.ApiController;
import com.pousadamarealta.domain.PriceCalculator;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.ArrayList;
import java.util.List;

public final class App {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL");
        String dbUser = getEnv("DB_USER");
        String dbPassword = getEnv("DB_PASSWORD");
        int port = Integer.parseInt(getEnv("PORT", "7070"));
        boolean testMode = Boolean.parseBoolean(getEnv("HOTEL_TEST_MODE", "false"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new JdbcRoomRepository(database.getDataSource());
        ReservationRepository reservationRepository = new JdbcReservationRepository(database.getDataSource());
        MessageRepository messageRepository = new JdbcMessageRepository(database.getDataSource());

        MessageProcessor messageProcessor = createMessageProcessor(testMode);
        MessageService messageService = new MessageService(messageRepository, messageProcessor);

        PriceCalculator priceCalculator = new PriceCalculator();
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService, priceCalculator);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            reservationService, roomRepository, messageService, reportService);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(port);

        app.get("/rooms", apiController::getRooms);
        app.post("/reservations", apiController::createReservation);
        app.get("/reservations/{id}", apiController::getReservation);
        app.post("/reservations/{id}/confirmation", apiController::confirmReservation);
        app.post("/reservations/{id}/cancellation", apiController::cancelReservation);
        app.post("/reservations/{id}/check-in", apiController::checkIn);
        app.post("/reservations/{id}/check-out", apiController::checkOut);
        app.get("/reservations/{id}/document", apiController::getDocument);
        app.get("/messages", apiController::getMessages);
        app.get("/reports/occupancy", apiController::getOccupancyReport);
        app.get("/reports/revenue", apiController::getRevenueReport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        System.out.println("Server started on port " + port);
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

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Environment variable " + name + " is not set");
        }
        return value;
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}
