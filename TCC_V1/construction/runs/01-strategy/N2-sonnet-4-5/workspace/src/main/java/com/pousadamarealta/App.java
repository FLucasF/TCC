package com.pousadamarealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.*;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import javax.sql.DataSource;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DataSource dataSource = Database.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initializeRooms();

        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService
        );
        ReportService reportService = new ReportService(reservationRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", roomController::getAllRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAllMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.exception(InvalidRequestException.class, ExceptionHandler::handleInvalidRequest);
        app.exception(RoomNotFoundException.class, ExceptionHandler::handleRoomNotFound);
        app.exception(ReservationNotFoundException.class, ExceptionHandler::handleReservationNotFound);
        app.exception(RoomUnavailableException.class, ExceptionHandler::handleRoomUnavailable);
        app.exception(OperationNotAllowedException.class, ExceptionHandler::handleOperationNotAllowed);
        app.exception(CapacityExceededException.class, ExceptionHandler::handleCapacityExceeded);
        app.exception(JsonProcessingException.class, ExceptionHandler::handleJsonProcessing);
        app.exception(Exception.class, ExceptionHandler::handleGenericException);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.shutdown();
        }));
    }
}
