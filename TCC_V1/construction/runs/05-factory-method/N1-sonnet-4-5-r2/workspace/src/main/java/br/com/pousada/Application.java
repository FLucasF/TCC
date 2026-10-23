package br.com.pousada;

import br.com.pousada.api.*;
import br.com.pousada.exception.*;
import br.com.pousada.repository.MessageRepository;
import br.com.pousada.repository.ReservationRepository;
import br.com.pousada.repository.RoomRepository;
import br.com.pousada.service.DocumentService;
import br.com.pousada.service.NotificationService;
import br.com.pousada.service.ReportService;
import br.com.pousada.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Application {
    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        RoomRepository roomRepository = new RoomRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);

        DocumentService documentService = new DocumentService();
        NotificationService notificationService = new NotificationService(messageRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, documentService, notificationService);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        int port = getPort();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, true));
        });

        app.exception(InvalidRequestException.class, (e, ctx) ->
            handleError(ctx, 400, "INVALID_REQUEST", e.getMessage()));

        app.exception(RoomNotFoundException.class, (e, ctx) ->
            handleError(ctx, 404, "ROOM_NOT_FOUND", e.getMessage()));

        app.exception(ReservationNotFoundException.class, (e, ctx) ->
            handleError(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage()));

        app.exception(RoomUnavailableException.class, (e, ctx) ->
            handleError(ctx, 409, "ROOM_UNAVAILABLE", e.getMessage()));

        app.exception(OperationNotAllowedException.class, (e, ctx) ->
            handleError(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage()));

        app.exception(CapacityExceededException.class, (e, ctx) ->
            handleError(ctx, 422, "CAPACITY_EXCEEDED", e.getMessage()));

        app.exception(IllegalArgumentException.class, (e, ctx) ->
            handleError(ctx, 400, "INVALID_REQUEST", e.getMessage()));

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

        System.out.println("Pousada Maré Alta system started on port " + port);
    }

    private static DataSource createDataSource() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new RuntimeException("Database configuration missing. Set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }

    private static void handleError(Context ctx, int status, String error, String message) {
        ctx.status(status).json(new ErrorResponse(error, message));
    }
}
