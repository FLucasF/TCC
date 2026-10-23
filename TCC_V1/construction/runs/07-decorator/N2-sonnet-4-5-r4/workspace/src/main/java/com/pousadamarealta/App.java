package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.*;
import com.pousadamarealta.messaging.*;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.*;
import com.pousadamarealta.util.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class App {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        DatabaseInitializer dbInitializer = new DatabaseInitializer(dataSource);
        dbInitializer.initialize();

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initializeRooms();

        MessageProcessor messageProcessor = createMessageProcessor();

        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            messageRepository,
            roomService,
            messageProcessor
        );
        OccupancyReportService occupancyReportService = new OccupancyReportService(
            reservationService,
            roomService
        );
        RevenueReportService revenueReportService = new RevenueReportService(reservationService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(occupancyReportService, revenueReportService);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.jsonMapper(new JavalinJackson(mapper, true));
        }).start(port);

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

        ExceptionHandler.register(app);
    }

    private static DataSource createDataSource() {
        String url = Config.get("DB_URL");
        String user = Config.get("DB_USER");
        String password = Config.get("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Database configuration missing");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static MessageProcessor createMessageProcessor() {
        List<MessageRule> rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        boolean testMode = "true".equalsIgnoreCase(Config.get("HOTEL_TEST_MODE"));

        if (testMode) {
            rules.add(new TestModeRule(true));
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageProcessor(rules);
    }

    private static int getPort() {
        String portEnv = Config.get("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }
}
