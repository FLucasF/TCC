package com.pousadamareaalta.controller;

import com.pousadamareaalta.dto.*;
import com.pousadamareaalta.exception.ApiException;
import com.pousadamareaalta.model.*;
import com.pousadamareaalta.service.*;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;

    public ReservationController(RoomService roomService, ReservationService reservationService,
                               MessageService messageService, ReportService reportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", this::listRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::listMessages);
        app.get("/reports/occupancy", this::occupancyReport);
        app.get("/reports/revenue", this::revenueReport);
        app.get("/reports/tourism-tax", this::tourismTaxReport);
        app.get("/reports/rate-plans", this::ratePlansReport);
        app.exception(ApiException.class, this::handleApiException);
        app.exception(Exception.class, this::handleException);
    }

    private void listRooms(io.javalin.http.Context ctx) {
        var rooms = roomService.getAllRooms();
        var dtos = rooms.stream()
            .map(RoomDTO::fromRoom)
            .toList();
        ctx.json(dtos);
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            LocalDate checkIn = parseDate(req.checkIn);
            LocalDate checkOut = parseDate(req.checkOut);

            Reservation res = reservationService.createReservation(
                req.roomNumber,
                req.guestName,
                req.guestEmail,
                req.guestCpf,
                checkIn,
                checkOut,
                req.guests,
                RatePlan.valueOf(req.ratePlan)
            );

            ctx.status(201);
            ctx.json(ReservationDTO.fromReservation(res));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format or rate plan"));
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = reservationService.getReservation(id);
        ctx.json(ReservationDTO.fromReservation(res));
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ConfirmationRequest req = ctx.bodyAsClass(ConfirmationRequest.class);

        Reservation res = reservationService.confirmReservation(id, parseMoney(req.amountPaid));
        ctx.json(ReservationDTO.fromReservation(res));
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = reservationService.cancelReservation(id);
        ctx.json(ReservationDTO.fromReservation(res));
    }

    private void checkIn(io.javalin.http.Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = reservationService.checkIn(id);
        ctx.json(ReservationDTO.fromReservation(res));
    }

    private void checkOut(io.javalin.http.Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Receipt receipt = reservationService.checkOut(id);
        ctx.json(ReceiptDTO.fromReceipt(receipt));
    }

    private void getDocument(io.javalin.http.Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Receipt receipt = reservationService.getDocument(id);
        ctx.json(ReceiptDTO.fromReceipt(receipt));
    }

    private void listMessages(io.javalin.http.Context ctx) {
        var messages = messageService.getMessages();
        var dtos = messages.stream()
            .map(MessageDTO::fromMessage)
            .toList();
        ctx.json(dtos);
    }

    private void occupancyReport(io.javalin.http.Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter required"));
            return;
        }

        try {
            LocalDate date = parseDate(dateStr);
            String report = reportService.generateOccupancyReport(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void revenueReport(io.javalin.http.Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.generateRevenueReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private void tourismTaxReport(io.javalin.http.Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.generateTourismTaxReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private void ratePlansReport(io.javalin.http.Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.generateRatePlansReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private void handleApiException(ApiException e, io.javalin.http.Context ctx) {
        ctx.status(e.getStatusCode());
        ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    private void handleException(Exception e, io.javalin.http.Context ctx) {
        ctx.status(500);
        ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
    }

    private LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr);
    }

    private BigDecimal parseMoney(Object value) {
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(value.toString());
    }
}
