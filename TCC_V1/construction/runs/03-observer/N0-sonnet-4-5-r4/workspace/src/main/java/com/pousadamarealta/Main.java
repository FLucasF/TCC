package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.MessageController;
import com.pousadamarealta.api.ReportController;
import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.api.RoomController;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.event.EventBus;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.event.handlers.FinanceNotificationHandler;
import com.pousadamarealta.domain.event.handlers.GuestNotificationHandler;
import com.pousadamarealta.domain.event.handlers.HousekeepingNotificationHandler;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.service.ReportService;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initializeSchema();

        try (Connection conn = database.getConnection()) {
            RoomRepository roomRepository = new RoomRepository(conn);
            roomRepository.initializeRooms();
        }

        EventBus eventBus = new EventBus();

        Connection connection = database.getConnection();
        MessageRepository messageRepository = new MessageRepository(connection);
        ReservationRepository reservationRepository = new ReservationRepository(connection);
        RoomRepository roomRepository = new RoomRepository(connection);

        MessageService messageService = new MessageService(messageRepository);
        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, roomService, eventBus);
        ReportService reportService = new ReportService(reservationRepository);

        GuestNotificationHandler guestHandler = new GuestNotificationHandler(messageService);
        HousekeepingNotificationHandler housekeepingHandler = new HousekeepingNotificationHandler(messageService);
        FinanceNotificationHandler financeHandler = new FinanceNotificationHandler(messageService);

        eventBus.subscribe(ReservationConfirmedEvent.class, guestHandler);
        eventBus.subscribe(ReservationConfirmedEvent.class, housekeepingHandler.onConfirmed());
        eventBus.subscribe(ReservationConfirmedEvent.class, financeHandler.onConfirmed());
        eventBus.subscribe(ReservationCancelledEvent.class, housekeepingHandler.onCancelled());
        eventBus.subscribe(ReservationCancelledEvent.class, financeHandler.onCancelled());

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
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }
}
