package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.messaging.*;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.MessageController;
import com.pousadamarealta.web.ReportController;
import com.pousadamarealta.web.ReservationController;
import com.pousadamarealta.web.RoomController;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.postgresql.ds.PGSimpleDataSource;

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

        MessageProcessor messageProcessor = createMessageProcessor();
        MessageService messageService = new MessageService(messageRepository, messageProcessor);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService, roomRepository);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", roomController::listRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::listMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
    }

    private static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration missing. " +
                    "Set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
        }

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        String jdbcUrl = dbUrl.replace("jdbc:", "");
        dataSource.setUrl(dbUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPassword);

        return dataSource;
    }

    private static MessageProcessor createMessageProcessor() {
        List<MessageRule> rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageProcessor(rules);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }
}
