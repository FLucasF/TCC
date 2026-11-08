package com.pousadamarealta;

import com.pousadamarealta.config.DatabaseConfig;
import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.model.RoomType;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.EventPublisher;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import com.pousadamarealta.service.listeners.*;
import com.pousadamarealta.web.controller.MessageController;
import com.pousadamarealta.web.controller.ReportController;
import com.pousadamarealta.web.controller.ReservationController;
import com.pousadamarealta.web.controller.RoomController;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DataSource dataSource = DatabaseConfig.createDataSource(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        try {
            roomRepository.createTable();
            reservationRepository.createTable();
            messageRepository.createTable();

            initializeRooms(roomRepository);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        EventPublisher eventPublisher = new EventPublisher();

        MessageService messageService = new MessageService(messageRepository);
        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            eventPublisher
        );

        eventPublisher.register(ReservationConfirmedEvent.class,
            new GuestNotificationListener(messageService));
        eventPublisher.register(ReservationConfirmedEvent.class,
            new HousekeepingNotificationListener(messageService));
        eventPublisher.register(ReservationConfirmedEvent.class,
            new FinanceNotificationListener(messageService));

        eventPublisher.register(ReservationCancelledEvent.class,
            new HousekeepingCancellationListener(messageService));
        eventPublisher.register(ReservationCancelledEvent.class,
            new FinanceCancellationListener(messageService));

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reservationService, roomService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
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

        System.out.println("Pousada Maré Alta - Reservation System");
        System.out.println("Server started on port " + port);
    }

    private static void initializeRooms(RoomRepository roomRepository) throws SQLException {
        roomRepository.insert(new Room("101", RoomType.STANDARD));
        roomRepository.insert(new Room("102", RoomType.STANDARD));
        roomRepository.insert(new Room("201", RoomType.DELUXE));
        roomRepository.insert(new Room("202", RoomType.DELUXE));
        roomRepository.insert(new Room("301", RoomType.SUITE));
    }
}
