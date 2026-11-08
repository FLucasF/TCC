package br.com.marealta;

import br.com.marealta.api.controller.MessageController;
import br.com.marealta.api.controller.ReportController;
import br.com.marealta.api.controller.ReservationController;
import br.com.marealta.api.controller.RoomController;
import br.com.marealta.api.dto.ErrorResponse;
import br.com.marealta.database.Database;
import br.com.marealta.pricing.PricingService;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RatePlansReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.report.TourismTaxReport;
import br.com.marealta.repository.MessageRepository;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.DocumentService;
import br.com.marealta.service.MessageService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnprocessableContentResponse;
import io.javalin.json.JavalinJackson;

public class App {
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

        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, messageService
        );
        DocumentService documentService = new DocumentService();

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService, documentService, roomRepository
        );
        MessageController messageController = new MessageController(messageService);

        OccupancyReport occupancyReport = new OccupancyReport(reservationRepository);
        RevenueReport revenueReport = new RevenueReport(reservationRepository);
        TourismTaxReport tourismTaxReport = new TourismTaxReport(reservationRepository);
        RatePlansReport ratePlansReport = new RatePlansReport(reservationRepository);
        ReportController reportController = new ReportController(
            occupancyReport, revenueReport, tourismTaxReport, ratePlansReport
        );

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", roomController::listRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::listMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        });

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request parameters"));
        });

        app.exception(BadRequestResponse.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid ID format"));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            if (!ctx.res().isCommitted()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor rodando na porta " + port);
    }
}
