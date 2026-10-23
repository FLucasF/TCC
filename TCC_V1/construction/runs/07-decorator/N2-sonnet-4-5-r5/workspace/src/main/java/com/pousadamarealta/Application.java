package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.handler.MessageHandler;
import com.pousadamarealta.api.handler.ReportHandler;
import com.pousadamarealta.api.handler.ReservationHandler;
import com.pousadamarealta.api.handler.RoomHandler;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.database.MessageDao;
import com.pousadamarealta.database.ReservationDao;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.messaging.MessagePipeline;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import com.pousadamarealta.reports.OccupancyReport;
import com.pousadamarealta.reports.RevenueReport;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.PricingCalculator;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.ArrayList;
import java.util.List;

public class Application {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = getEnv("DB_USER", "postgres");
        String dbPassword = getEnv("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(getEnv("PORT", "7070"));
        boolean testMode = "true".equalsIgnoreCase(getEnv("HOTEL_TEST_MODE", "false"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomDao roomDao = new RoomDao(database);
        ReservationDao reservationDao = new ReservationDao(database);
        MessageDao messageDao = new MessageDao(database);

        MessagePipeline messagePipeline = createMessagePipeline(testMode);
        MessageService messageService = new MessageService(messageDao, messagePipeline);

        PricingCalculator pricingCalculator = new PricingCalculator();
        ReservationService reservationService = new ReservationService(
            reservationDao, roomDao, pricingCalculator, messageService);

        OccupancyReport occupancyReport = new OccupancyReport(reservationDao, roomDao);
        RevenueReport revenueReport = new RevenueReport(reservationDao);

        RoomHandler roomHandler = new RoomHandler(roomDao);
        ReservationHandler reservationHandler = new ReservationHandler(reservationService, roomDao);
        MessageHandler messageHandler = new MessageHandler(messageService);
        ReportHandler reportHandler = new ReportHandler(occupancyReport, revenueReport);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", roomHandler::getRooms);

        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);

        app.get("/messages", messageHandler::getMessages);

        app.get("/reports/occupancy", reportHandler::getOccupancyReport);
        app.get("/reports/revenue", reportHandler::getRevenueReport);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }

    private static MessagePipeline createMessagePipeline(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRedirectRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessagePipeline(rules);
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
