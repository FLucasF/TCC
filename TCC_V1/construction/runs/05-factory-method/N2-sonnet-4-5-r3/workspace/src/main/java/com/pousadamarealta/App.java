package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.*;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.database.DatabaseMigration;
import com.pousadamarealta.document.DocumentGeneratorFactory;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.EmailService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.sql.SQLException;

public class App {
    public static void main(String[] args) throws SQLException {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        DatabaseMigration migration = new DatabaseMigration(database);
        migration.migrate();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        DocumentGeneratorFactory documentGeneratorFactory = new DocumentGeneratorFactory();

        RoomService roomService = new RoomService(roomRepository);
        EmailService emailService = new EmailService(messageRepository, documentGeneratorFactory);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomService,
            emailService,
            documentGeneratorFactory
        );
        ReportService reportService = new ReportService(reservationRepository, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
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
    }
}
