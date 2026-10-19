package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationDocument;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReservationApi {
    private final Javalin app;
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public ReservationApi(
        Javalin app,
        ReservationService reservationService,
        ReportService reportService,
        RoomRepository roomRepository,
        MessageRepository messageRepository
    ) {
        this.app = app;
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
    }

    public void setup() {
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

    private void getRooms(Context ctx) {
        ctx.json(roomRepository.findAll().stream().map(RoomResponse::from).toList());
    }

    private void createReservation(Context ctx) {
        try {
            ReservationRequest req = ctx.bodyAsClass(ReservationRequest.class);
            validateReservationRequest(req);

            LocalDate checkIn = LocalDate.parse(req.checkIn());
            LocalDate checkOut = LocalDate.parse(req.checkOut());
            RatePlan ratePlan = RatePlan.valueOf(req.ratePlan());

            Reservation reservation = reservationService.createReservation(
                req.roomNumber(),
                req.guestName(),
                req.guestEmail(),
                req.guestCpf(),
                checkIn,
                checkOut,
                req.guests(),
                ratePlan
            );

            ctx.status(201).json(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (DateTimeParseException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
        } catch (IllegalStateException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request format");
        } catch (Exception e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request");
        }
    }

    private void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ReservationDocument doc = reservationService.getReservationDocument(id);
            ctx.json(DocumentResponse.from(doc));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            ReservationDocument doc = reservationService.getReservationDocument(id);
            ctx.json(DocumentResponse.from(doc));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void getMessages(Context ctx) {
        ctx.json(messageRepository.findAll().stream().map(MessageResponse::from).toList());
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "date parameter is required");
                return;
            }
            LocalDate date = LocalDate.parse(dateStr);
            String report = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "month parameter is required");
                return;
            }
            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private void validateReservationRequest(ReservationRequest req) {
        if (req.roomNumber() <= 0 || req.guestName() == null || req.guestEmail() == null ||
            req.guestCpf() == null || req.checkIn() == null || req.checkOut() == null ||
            req.guests() < 1 || req.ratePlan() == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }

    private void handleError(Context ctx, String errorCode) {
        int status = switch (errorCode) {
            case "ROOM_NOT_FOUND" -> 404;
            case "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE" -> 409;
            case "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };

        String message = switch (errorCode) {
            case "ROOM_NOT_FOUND" -> "Room not found";
            case "RESERVATION_NOT_FOUND" -> "Reservation not found";
            case "ROOM_UNAVAILABLE" -> "Room is not available for the selected dates";
            case "OPERATION_NOT_ALLOWED" -> "This operation is not allowed for the current reservation status";
            case "CAPACITY_EXCEEDED" -> "Number of guests exceeds room capacity";
            case "INVALID_REQUEST" -> "Invalid request";
            default -> "An error occurred";
        };

        error(ctx, status, errorCode, message);
    }

    private void error(Context ctx, int status, String errorCode, String message) {
        ctx.status(status).json(new ErrorResponse(errorCode, message));
    }
}
