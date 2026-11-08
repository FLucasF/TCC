package com.pousadamarealta.infrastructure.http;

import com.pousadamarealta.application.service.*;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.infrastructure.http.dto.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public ReservationController(ReservationService reservationService,
                                ReportService reportService,
                                RoomRepository roomRepository,
                                MessageRepository messageRepository) {
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
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

        app.exception(InvalidRequestException.class, (e, ctx) ->
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage())));
        app.exception(RoomNotFoundException.class, (e, ctx) ->
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage())));
        app.exception(ReservationNotFoundException.class, (e, ctx) ->
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage())));
        app.exception(RoomUnavailableException.class, (e, ctx) ->
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage())));
        app.exception(OperationNotAllowedException.class, (e, ctx) ->
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage())));
        app.exception(CapacityExceededException.class, (e, ctx) ->
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage())));
    }

    private void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
            .map(RoomResponse::from)
            .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequest(request);

            LocalDate checkIn = parseDate(request.checkIn());
            LocalDate checkOut = parseDate(request.checkOut());
            RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                checkIn,
                checkOut,
                request.guests(),
                ratePlan
            );

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.status(201).json(ReservationResponse.from(reservation, room));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.findById(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    private void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    private void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    private void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    private void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(ReceiptResponse.from(reservation, room));
    }

    private void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.findById(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(ReceiptResponse.from(reservation, room));
    }

    private void getMessages(Context ctx) {
        List<MessageResponse> messages = messageRepository.findAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new InvalidRequestException("Missing 'date' parameter");
        }

        LocalDate date = parseDate(dateStr);
        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing 'month' parameter");
        }

        YearMonth month = parseMonth(monthStr);
        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null ||
            request.guests() == null || request.ratePlan() == null) {
            throw new InvalidRequestException("Missing required fields");
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }
    }

    private YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }
}
