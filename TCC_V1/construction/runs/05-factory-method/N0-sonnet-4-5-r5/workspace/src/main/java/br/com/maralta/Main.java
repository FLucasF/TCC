package br.com.maralta;

import br.com.maralta.dto.*;
import br.com.maralta.exception.ApiException;
import br.com.maralta.model.Document;
import br.com.maralta.repository.*;
import br.com.maralta.service.*;
import br.com.maralta.util.DatabaseInitializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Main {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();
        EmailService emailService = new EmailService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, documentService, emailService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            config.jsonMapper(new JavalinJackson(mapper, true));
        });

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll();
            ctx.json(rooms.stream().map(RoomResponse::from).toList());
        });

        app.post("/reservations", ctx -> {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
            validateRequest(request);

            var reservation = reservationService.createReservation(request.toReservation());
            ctx.status(201);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.get("/reservations/{id}", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            var reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            var reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            var reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Document document = reservationService.checkOut(id);
            ctx.json(document);
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            var reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.get("/reservations/{id}/document", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Document document = reservationService.getDocument(id);
            ctx.json(document);
        });

        app.get("/messages", ctx -> {
            var messages = messageRepository.findAll();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                throw ApiException.invalidRequest("Date parameter is required");
            }

            LocalDate date = parseDate(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                throw ApiException.invalidRequest("Month parameter is required");
            }

            YearMonth month = parseMonth(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        });

        app.start(port);
        System.out.println("Pousada Maré Alta server started on port " + port);
    }

    private static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database environment variables not set");
        }

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(dbUrl);
        ds.setUser(dbUser);
        ds.setPassword(dbPassword);

        return ds;
    }

    private static int getPort() {
        String portStr = System.getenv("PORT");
        return portStr != null ? Integer.parseInt(portStr) : 7070;
    }

    private static void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getCheckIn() == null ||
            request.getCheckOut() == null || request.getRatePlan() == null) {
            throw ApiException.invalidRequest("Missing required fields");
        }
    }

    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private static YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Invalid month format. Expected: yyyy-MM");
        }
    }
}
