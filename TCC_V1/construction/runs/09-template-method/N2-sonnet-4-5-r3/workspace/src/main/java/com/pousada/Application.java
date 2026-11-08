package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.MessageController;
import com.pousada.api.ReportController;
import com.pousada.api.ReservationController;
import com.pousada.api.RoomController;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.repository.Database;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.DocumentService;
import com.pousada.service.PricingService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    private static String getConfig(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null) {
            return systemProperty;
        }
        return System.getenv().getOrDefault(key, defaultValue);
    }

    public static void main(String[] args) {
        String dbUrl = getConfig("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = getConfig("DB_USER", "postgres");
        String dbPassword = getConfig("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(getConfig("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initializeSchema();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        initializeRooms(roomRepository);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationRepository, roomRepository, messageRepository, pricingService, documentService
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reservationRepository, roomRepository);

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
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);
    }

    private static void initializeRooms(RoomRepository roomRepository) {
        roomRepository.save(new Room("101", RoomType.STANDARD));
        roomRepository.save(new Room("102", RoomType.STANDARD));
        roomRepository.save(new Room("201", RoomType.DELUXE));
        roomRepository.save(new Room("202", RoomType.DELUXE));
        roomRepository.save(new Room("301", RoomType.SUITE));
    }
}
