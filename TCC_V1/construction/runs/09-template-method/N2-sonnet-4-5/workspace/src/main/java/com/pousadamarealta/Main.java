package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.*;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.repository.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = getEnv("DB_USER", "postgres");
        String dbPassword = getEnv("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(getEnv("PORT", "7070"));

        Database.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        initializeRooms(roomRepository);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            pricingService,
            documentService,
            messageService
        );

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService,
            roomRepository,
            documentService
        );
        ReportController reportController = new ReportController(reservationRepository, 5);
        MessageController messageController = new MessageController(messageRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

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
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.close();
        }));
    }

    private static void initializeRooms(RoomRepository repository) {
        repository.save(new Room("101", RoomType.STANDARD));
        repository.save(new Room("102", RoomType.STANDARD));
        repository.save(new Room("201", RoomType.DELUXE));
        repository.save(new Room("202", RoomType.DELUXE));
        repository.save(new Room("301", RoomType.SUITE));
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
