package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.domain.enums.RoomType;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import com.pousadamarealta.service.event.EventPublisher;
import com.pousadamarealta.service.event.listeners.FinanceNotificationListener;
import com.pousadamarealta.service.event.listeners.GuestNotificationListener;
import com.pousadamarealta.service.event.listeners.HousekeepingNotificationListener;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
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

        EventPublisher eventPublisher = new EventPublisher();
        eventPublisher.subscribe(new GuestNotificationListener(messageRepository, roomRepository));
        eventPublisher.subscribe(new HousekeepingNotificationListener(messageRepository));
        eventPublisher.subscribe(new FinanceNotificationListener(messageRepository));

        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, eventPublisher
        );
        DocumentService documentService = new DocumentService(roomRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, documentService
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

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

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.close();
        }));
    }

    private static void initializeRooms(RoomRepository roomRepository) {
        roomRepository.save(new Room("101", RoomType.STANDARD));
        roomRepository.save(new Room("102", RoomType.STANDARD));
        roomRepository.save(new Room("201", RoomType.DELUXE));
        roomRepository.save(new Room("202", RoomType.DELUXE));
        roomRepository.save(new Room("301", RoomType.SUITE));
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
