package br.com.marealta;

import br.com.marealta.controller.*;
import br.com.marealta.database.DatabaseInitializer;
import br.com.marealta.dto.ErrorResponse;
import br.com.marealta.exception.PousadaException;
import br.com.marealta.repository.MessageRepository;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Application {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initializeRooms();

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService(pricingService);
        EmailService emailService = new EmailService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            pricingService,
            documentService,
            emailService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService,
            roomRepository,
            documentService
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        app.exception(PousadaException.class, (e, ctx) -> {
            ctx.status(e.getHttpStatus())
               .json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });

        app.start(port);

        System.out.println("Pousada Maré Alta system running on port " + port);
    }

    private static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database configuration not found. Set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value: " + portEnv + ". Using default 7070");
            }
        }
        return 7070;
    }
}
