package br.com.pousada.marealta;

import br.com.pousada.marealta.controller.MessageController;
import br.com.pousada.marealta.controller.ReportController;
import br.com.pousada.marealta.controller.ReservationController;
import br.com.pousada.marealta.controller.RoomController;
import br.com.pousada.marealta.dto.ErrorResponse;
import br.com.pousada.marealta.exception.AppException;
import br.com.pousada.marealta.repository.MessageRepository;
import br.com.pousada.marealta.repository.ReservationRepository;
import br.com.pousada.marealta.repository.RoomRepository;
import br.com.pousada.marealta.service.DocumentService;
import br.com.pousada.marealta.service.EmailService;
import br.com.pousada.marealta.service.ReportService;
import br.com.pousada.marealta.service.ReservationService;
import br.com.pousada.marealta.util.DatabaseInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DatabaseInitializer db = new DatabaseInitializer(dbUrl, dbUser, dbPassword);
        db.initialize();

        RoomRepository roomRepository = new RoomRepository(db);
        ReservationRepository reservationRepository = new ReservationRepository(db);
        MessageRepository messageRepository = new MessageRepository(db);

        DocumentService documentService = new DocumentService();
        EmailService emailService = new EmailService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            documentService,
            emailService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService,
            reservationRepository,
            roomRepository
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

        app.exception(AppException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });

        app.start(port);
    }
}
