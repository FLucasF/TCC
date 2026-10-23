package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.dto.*;
import com.pousada.application.ReportService;
import com.pousada.application.ReservationService;
import com.pousada.domain.model.Document;
import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.Reservation;
import com.pousada.domain.repository.EmailRepository;
import com.pousada.domain.repository.ReservationRepository;
import com.pousada.domain.repository.RoomRepository;
import com.pousada.infrastructure.Database;
import com.pousada.infrastructure.EmailRepositoryImpl;
import com.pousada.infrastructure.ReservationRepositoryImpl;
import com.pousada.infrastructure.RoomRepositoryImpl;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Application {

    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepositoryImpl(database.getDataSource());
        ReservationRepository reservationRepository = new ReservationRepositoryImpl(database.getDataSource());
        EmailRepository emailRepository = new EmailRepositoryImpl(database.getDataSource());

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, emailRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll();
            ctx.json(rooms.stream().map(RoomResponse::from).toList());
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

                validateRequiredFields(req);

                LocalDate checkIn = parseDate(req.checkIn(), "checkIn");
                LocalDate checkOut = parseDate(req.checkOut(), "checkOut");

                if (!checkOut.isAfter(checkIn)) {
                    throw new ValidationException("Check-out must be after check-in");
                }

                RatePlan ratePlan = parseRatePlan(req.ratePlan());

                Reservation reservation = reservationService.createReservation(
                    req.roomNumber(),
                    req.guestName(),
                    req.guestEmail(),
                    req.guestCpf(),
                    req.companyCnpj(),
                    req.companyName(),
                    req.guestPassport(),
                    req.guestCountry(),
                    checkIn,
                    checkOut,
                    req.guests(),
                    ratePlan
                );

                ctx.status(201).json(ReservationResponse.from(reservation));

            } catch (ValidationException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (ReservationService.InvalidRequestException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request: " + e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Document document = reservationService.checkOut(id);
                ctx.json(DocumentResponse.from(document));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Document document = reservationService.getDocument(id);
                ctx.json(DocumentResponse.from(document));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            var emails = emailRepository.findAll();
            ctx.json(emails.stream().map(EmailResponse::from).toList());
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                    return;
                }

                LocalDate date = parseDate(dateStr, "date");
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);

            } catch (ValidationException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                if (!monthStr.matches("\\d{4}-\\d{2}")) {
                    throw new ValidationException("Invalid month format, expected yyyy-MM");
                }

                String[] parts = monthStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                String csv = reportService.generateRevenueReport(year, month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);

            } catch (ValidationException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.start(port);
        System.out.println("Server started on port " + port);
    }

    private static void validateRequiredFields(CreateReservationRequest req) {
        if (req.roomNumber() == null || req.guestName() == null || req.guestEmail() == null ||
            req.checkIn() == null || req.checkOut() == null || req.guests() == null || req.ratePlan() == null) {
            throw new ValidationException("Missing required fields");
        }
    }

    private static LocalDate parseDate(String dateStr, String fieldName) {
        if (dateStr == null) {
            throw new ValidationException("Missing " + fieldName);
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format for " + fieldName + ", expected yyyy-MM-dd");
        }
    }

    private static RatePlan parseRatePlan(String ratePlan) {
        try {
            return RatePlan.valueOf(ratePlan);
        } catch (Exception e) {
            throw new ValidationException("Invalid rate plan: " + ratePlan);
        }
    }

    private static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
