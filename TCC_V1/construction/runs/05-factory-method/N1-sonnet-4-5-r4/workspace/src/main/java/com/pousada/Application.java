package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.*;
import com.pousada.api.dto.ErrorResponse;
import com.pousada.infrastructure.Database;
import com.pousada.infrastructure.MessageRepository;
import com.pousada.infrastructure.ReservationRepository;
import com.pousada.infrastructure.RoomRepository;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        ReservationService reservationService = new ReservationService(
            roomRepository, reservationRepository, messageRepository
        );
        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
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

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
               .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
        });

        app.start(port);
        System.out.println("Pousada Maré Alta running on port " + port);
    }
}
