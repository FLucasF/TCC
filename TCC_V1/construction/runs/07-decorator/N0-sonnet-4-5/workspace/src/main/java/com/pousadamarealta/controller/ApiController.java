package com.pousadamarealta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.dto.*;
import com.pousadamarealta.messaging.MessagingService;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ApiController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessagingService messagingService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ApiController(RoomService roomService,
                        ReservationService reservationService,
                        MessagingService messagingService,
                        ReportService reportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messagingService = messagingService;
        this.reportService = reportService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
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

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(DateTimeParseException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomResponse> response = rooms.stream()
                .map(RoomResponse::from)
                .toList();
        ctx.json(response);
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateCreateReservationRequest(request);

            LocalDate checkIn = LocalDate.parse(request.getCheckIn());
            LocalDate checkOut = LocalDate.parse(request.getCheckOut());

            Reservation reservation = reservationService.createReservation(
                request.getRoomNumber(),
                request.getGuestName(),
                request.getGuestEmail(),
                request.getGuestCpf(),
                checkIn,
                checkOut,
                request.getGuests(),
                request.getRatePlan()
            );

            ctx.status(201).json(ReservationResponse.from(reservation));
        } catch (Exception e) {
            if (e instanceof DateTimeParseException) {
                throw new ReservationService.InvalidRequestException("Invalid date format");
            }
            throw e;
        }
    }

    private void validateCreateReservationRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getRoomNumber().isEmpty()) {
            throw new ReservationService.InvalidRequestException("Room number is required");
        }
        if (request.getGuestName() == null || request.getGuestName().isEmpty()) {
            throw new ReservationService.InvalidRequestException("Guest name is required");
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().isEmpty()) {
            throw new ReservationService.InvalidRequestException("Guest email is required");
        }
        if (request.getGuestCpf() == null || request.getGuestCpf().isEmpty()) {
            throw new ReservationService.InvalidRequestException("Guest CPF is required");
        }
        if (request.getCheckIn() == null || request.getCheckIn().isEmpty()) {
            throw new ReservationService.InvalidRequestException("Check-in date is required");
        }
        if (request.getCheckOut() == null || request.getCheckOut().isEmpty()) {
            throw new ReservationService.InvalidRequestException("Check-out date is required");
        }
        if (request.getGuests() == null) {
            throw new ReservationService.InvalidRequestException("Number of guests is required");
        }
        if (request.getRatePlan() == null) {
            throw new ReservationService.InvalidRequestException("Rate plan is required");
        }
    }

    private void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(ReceiptResponse.from(reservation, room));
    }

    private void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().doubleValue() == 0) {
            throw new ReservationService.OperationNotAllowedException("Receipt is only available for paid reservations");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(ReceiptResponse.from(reservation, room));
    }

    private void getMessages(Context ctx) {
        List<MessageResponse> messages = messagingService.getAllMessages().stream()
                .map(MessageResponse::from)
                .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isEmpty()) {
            throw new ReservationService.InvalidRequestException("Date parameter is required");
        }

        LocalDate date = LocalDate.parse(dateParam);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            throw new ReservationService.InvalidRequestException("Month parameter is required");
        }

        YearMonth month = YearMonth.parse(monthParam);
        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
