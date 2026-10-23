package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.MessageController;
import com.pousadamarealta.api.ReportController;
import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.api.RoomController;
import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.messaging.MessagingService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL");
        String dbUser = getEnv("DB_USER");
        String dbPassword = getEnv("DB_PASSWORD");
        int port = getPort();
        boolean testMode = isTestMode();

        DatabaseManager dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);
        dbManager.initialize();

        RoomRepository roomRepository = new RoomRepository(dbManager);
        ReservationRepository reservationRepository = new ReservationRepository(dbManager);
        MessageRepository messageRepository = new MessageRepository(dbManager);

        initializeRooms(roomRepository);

        MessagingService messagingService = new MessagingService(messageRepository, testMode);
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messagingService, documentService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService, roomRepository, documentService);
        MessageController messageController = new MessageController(messagingService);
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
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        });

        System.out.println("Server started on port " + port + " (test mode: " + testMode + ")");
    }

    private static void initializeRooms(RoomRepository roomRepository) {
        roomRepository.save(new Room(101, RoomType.STANDARD));
        roomRepository.save(new Room(102, RoomType.STANDARD));
        roomRepository.save(new Room(201, RoomType.DELUXE));
        roomRepository.save(new Room(202, RoomType.DELUXE));
        roomRepository.save(new Room(301, RoomType.SUITE));
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Environment variable " + name + " is required");
        }
        return value;
    }

    private static int getPort() {
        String portStr = System.getenv("PORT");
        if (portStr == null || portStr.isEmpty()) {
            return 7070;
        }
        return Integer.parseInt(portStr);
    }

    private static boolean isTestMode() {
        String testMode = System.getenv("HOTEL_TEST_MODE");
        return "true".equalsIgnoreCase(testMode);
    }
}
