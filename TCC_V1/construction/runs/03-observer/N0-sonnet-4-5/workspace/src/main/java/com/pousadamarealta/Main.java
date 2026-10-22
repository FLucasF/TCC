package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.*;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.events.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        EventPublisher eventPublisher = new EventPublisher();
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
                new GuestNotificationListener(messageRepository, roomRepository));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
                new HousekeepingNotificationListener(messageRepository));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
                new FinanceNotificationListener(messageRepository));
        eventPublisher.subscribe(ReservationCancelledEvent.class,
                new HousekeepingCancellationListener(messageRepository));
        eventPublisher.subscribe(ReservationCancelledEvent.class,
                new FinanceCancellationListener(messageRepository));

        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, pricingService, eventPublisher);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService, roomRepository);
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

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor rodando na porta " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }
}
