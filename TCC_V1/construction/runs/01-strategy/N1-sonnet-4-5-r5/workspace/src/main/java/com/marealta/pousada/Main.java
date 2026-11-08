package com.marealta.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marealta.pousada.controller.MessageController;
import com.marealta.pousada.controller.ReportController;
import com.marealta.pousada.controller.ReservationController;
import com.marealta.pousada.controller.RoomController;
import com.marealta.pousada.dto.ErrorResponse;
import com.marealta.pousada.exception.*;
import com.marealta.pousada.repository.MessageRepository;
import com.marealta.pousada.repository.ReservationRepository;
import com.marealta.pousada.repository.RoomRepository;
import com.marealta.pousada.service.MessageService;
import com.marealta.pousada.service.ReportService;
import com.marealta.pousada.service.ReservationService;
import com.marealta.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.json.JavalinJackson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Connection connection = initializeDatabase(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(connection);
        ReservationRepository reservationRepository = new ReservationRepository(connection);
        MessageRepository messageRepository = new MessageRepository(connection);

        MessageService messageService = new MessageService(messageRepository);
        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, roomService, messageService);
        ReportService reportService = new ReportService(reservationService, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", roomController::getAllRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getAllMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(BadRequestResponse.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
        });

        app.exception(NotFoundResponse.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("NOT_FOUND", "Resource not found"));
        });

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor rodando na porta " + port);
    }

    private static Connection initializeDatabase(String dbUrl, String dbUser, String dbPassword) {
        try {
            Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            RoomRepository roomRepository = new RoomRepository(connection);
            ReservationRepository reservationRepository = new ReservationRepository(connection);
            MessageRepository messageRepository = new MessageRepository(connection);

            roomRepository.createTable();
            reservationRepository.createTable();
            messageRepository.createTable();

            roomRepository.initializeRooms();

            System.out.println("Banco de dados inicializado com sucesso");

            return connection;
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco de dados: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }
}
