package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.api.dto.*;
import com.pousada.domain.*;
import com.pousada.repository.InMemoryRepository;
import com.pousada.service.DocumentGenerator;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ApiHandler {
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final InMemoryRepository repository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ApiHandler() {
        this.reservationService = new ReservationService();
        this.reportService = new ReportService();
        this.repository = InMemoryRepository.getInstance();
        this.objectMapper = new ObjectMapper();
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getRooms(io.javalin.http.Context ctx) {
        try {
            List<RoomResponse> rooms = repository.getAllRooms().stream()
                .map(RoomResponse::fromRoom)
                .sorted(Comparator.comparingInt((RoomResponse r) -> r.number))
                .toList();
            ctx.json(rooms);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.roomNumber == null || request.guestName == null || request.guestEmail == null ||
                request.checkIn == null || request.checkOut == null || request.guests == null ||
                request.ratePlan == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            GuestIdentification guestId = parseGuestIdentification(request);
            if (guestId == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid guest identification"));
                return;
            }

            LocalDate checkIn = parseDate(request.checkIn);
            LocalDate checkOut = parseDate(request.checkOut);
            if (checkIn == null || checkOut == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            Reservation.RatePlan ratePlan = Reservation.RatePlan.valueOf(request.ratePlan);

            Reservation reservation = reservationService.createReservation(
                request.roomNumber, request.guestName, request.guestEmail, guestId,
                checkIn, checkOut, request.guests, ratePlan);

            ctx.status(201);
            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            handleValidationException(ctx, e);
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = repository.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            ConfirmationRequest request = ctx.bodyAsClass(ConfirmationRequest.class);

            if (request.amountPaid == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "amountPaid is required"));
                return;
            }

            Reservation reservation = reservationService.confirmReservation(id, request.amountPaid);
            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            handleValidationException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            handleValidationException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void checkIn(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            handleValidationException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void checkOut(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.checkOut(id);
            ctx.json(DocumentResponse.fromDocument(document));
        } catch (ReservationService.ValidationException e) {
            handleValidationException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void getDocument(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.getDocument(id);
            ctx.json(DocumentResponse.fromDocument(document));
        } catch (ReservationService.ValidationException e) {
            handleValidationException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void getMessages(io.javalin.http.Context ctx) {
        try {
            List<MessageResponse> messages = repository.getAllMessages().stream()
                .map(MessageResponse::fromMessage)
                .toList();
            ctx.json(messages);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
                return;
            }

            LocalDate date = parseDate(dateStr);
            if (date == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            String report = reportService.generateOccupancyReport(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month"));
                return;
            }

            String report = reportService.generateRevenueReport(year, month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private GuestIdentification parseGuestIdentification(CreateReservationRequest request) {
        int count = 0;
        GuestIdentification result = null;

        if (request.guestCpf != null) {
            count++;
            result = new CpfIdentification(request.guestCpf);
        }
        if (request.companyCnpj != null && request.companyName != null) {
            count++;
            result = new CnpjIdentification(request.companyCnpj, request.companyName);
        }
        if (request.guestPassport != null && request.guestCountry != null) {
            count++;
            result = new PassportIdentification(request.guestPassport, request.guestCountry);
        }

        if (count != 1) {
            return null;
        }
        return result;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void handleValidationException(io.javalin.http.Context ctx, ReservationService.ValidationException e) {
        String code = e.getErrorCode();
        ctx.status(getStatusForErrorCode(code));
        ctx.json(new ErrorResponse(code, e.getMessage()));
    }

    private int getStatusForErrorCode(String code) {
        return switch (code) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "CAPACITY_EXCEEDED" -> 422;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            default -> 400;
        };
    }
}
