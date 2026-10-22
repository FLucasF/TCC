package com.pousadamareaalta.api;

import com.pousadamareaalta.api.dto.*;
import com.pousadamareaalta.exception.*;
import com.pousadamareaalta.model.RatePlan;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.service.MessageService;
import com.pousadamareaalta.service.ReportService;
import com.pousadamareaalta.service.ReservationService;
import com.pousadamareaalta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ApiController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;
    private final ApiMapper mapper;

    public ApiController(RoomService roomService, ReservationService reservationService,
                        MessageService messageService, ReportService reportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
        this.mapper = new ApiMapper(roomService);
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

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });
    }

    private void getRooms(Context ctx) {
        var rooms = roomService.getAllRooms().stream()
            .map(mapper::toRoomDTO)
            .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            LocalDate checkIn = LocalDate.parse(req.checkIn(), DATE_FORMATTER);
            LocalDate checkOut = LocalDate.parse(req.checkOut(), DATE_FORMATTER);
            RatePlan ratePlan = RatePlan.valueOf(req.ratePlan());

            Reservation reservation = reservationService.createReservation(
                req.roomNumber(),
                req.guestName(),
                req.guestEmail(),
                req.guestCpf(),
                checkIn,
                checkOut,
                req.guests(),
                ratePlan,
                req.companyCode()
            );

            ctx.status(201).json(mapper.toReservationDTO(reservation));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid request data");
        }
    }

    private void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(mapper.toReservationDTO(reservation));
    }

    private void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(mapper.toReservationDTO(reservation));
    }

    private void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(mapper.toReservationDTO(reservation));
    }

    private void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(mapper.toReservationDTO(reservation));
    }

    private void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(mapper.toReceiptDTO(reservation));
    }

    private void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(mapper.toReceiptDTO(reservation));
    }

    private void getMessages(Context ctx) {
        var messages = messageService.getAllMessages().stream()
            .map(mapper::toMessageDTO)
            .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                throw new InvalidRequestException("Date parameter is required");
            }

            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                throw new InvalidRequestException("Month parameter is required");
            }

            YearMonth yearMonth = YearMonth.parse(monthStr, MONTH_FORMATTER);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }
}
