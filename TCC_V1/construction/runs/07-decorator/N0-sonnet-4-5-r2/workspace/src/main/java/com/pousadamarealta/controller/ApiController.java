package com.pousadamarealta.controller;

import com.pousadamarealta.controller.dto.CreateReservationRequest;
import com.pousadamarealta.controller.dto.ErrorResponse;
import com.pousadamarealta.domain.exception.ApiException;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ApiController {

    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;

    public ApiController(RoomRepository roomRepository,
                        ReservationService reservationService,
                        MessageService messageService,
                        ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
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

        app.exception(ApiException.class, this::handleApiException);
        app.exception(Exception.class, this::handleGenericException);
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequiredFields(request);

            Reservation reservation = new Reservation();
            reservation.setRoomNumber(request.getRoomNumber());
            reservation.setGuestName(request.getGuestName());
            reservation.setGuestEmail(request.getGuestEmail());
            reservation.setGuestCpf(request.getGuestCpf());

            try {
                reservation.setCheckIn(LocalDate.parse(request.getCheckIn()));
                reservation.setCheckOut(LocalDate.parse(request.getCheckOut()));
            } catch (DateTimeParseException e) {
                throw new ApiException("INVALID_REQUEST", "Invalid date format", 400);
            }

            if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
                reservation.getCheckOut().isEqual(reservation.getCheckIn())) {
                throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
            }

            reservation.setGuests(request.getGuests());

            if (reservation.getGuests() < 1) {
                throw new ApiException("INVALID_REQUEST", "At least one guest is required", 400);
            }

            try {
                reservation.setRatePlan(RatePlan.valueOf(request.getRatePlan()));
            } catch (IllegalArgumentException e) {
                throw new ApiException("INVALID_REQUEST", "Invalid rate plan", 400);
            }

            Reservation created = reservationService.createReservation(reservation);
            ctx.status(201).json(created);

        } catch (Exception e) {
            if (e instanceof ApiException) {
                throw e;
            }
            throw new ApiException("INVALID_REQUEST", "Invalid request body", 400);
        }
    }

    private void validateRequiredFields(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getGuestCpf() == null ||
            request.getCheckIn() == null || request.getCheckOut() == null ||
            request.getGuests() == null || request.getRatePlan() == null) {
            throw new ApiException("INVALID_REQUEST", "Missing required fields", 400);
        }
    }

    private void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(reservation);
    }

    private void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(reservation);
    }

    private void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(reservation);
    }

    private void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(reservation);
    }

    private void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Document document = reservationService.getDocument(id);
        ctx.json(document);
    }

    private void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(document);
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new ApiException("INVALID_REQUEST", "Missing date parameter", 400);
        }

        try {
            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new ApiException("INVALID_REQUEST", "Invalid date format", 400);
        }
    }

    private void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new ApiException("INVALID_REQUEST", "Missing month parameter", 400);
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(
                yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new ApiException("INVALID_REQUEST", "Invalid month format", 400);
        }
    }

    private void handleApiException(ApiException e, Context ctx) {
        ctx.status(e.getStatusCode())
           .json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    private void handleGenericException(Exception e, Context ctx) {
        e.printStackTrace();
        ctx.status(500)
           .json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
    }
}
