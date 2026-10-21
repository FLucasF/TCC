package com.pousadamarealta.controller;

import com.pousadamarealta.dto.*;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
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
    private final DocumentService documentService;
    private final MessageRepository messageRepository;
    private final ReportService reportService;

    public ApiController(RoomRepository roomRepository, ReservationService reservationService,
                        DocumentService documentService, MessageRepository messageRepository,
                        ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.documentService = documentService;
        this.messageRepository = messageRepository;
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

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(DateTimeParseException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });
    }

    private void getRooms(Context ctx) {
        List<RoomDto> rooms = roomRepository.findAll().stream()
                .map(RoomDto::from)
                .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequired(request.roomNumber(), "roomNumber");
        validateRequired(request.guestName(), "guestName");
        validateRequired(request.guestEmail(), "guestEmail");
        validateRequired(request.guestCpf(), "guestCpf");
        validateRequired(request.checkIn(), "checkIn");
        validateRequired(request.checkOut(), "checkOut");
        validateRequired(request.guests(), "guests");
        validateRequired(request.ratePlan(), "ratePlan");

        LocalDate checkIn = parseDate(request.checkIn());
        LocalDate checkOut = parseDate(request.checkOut());
        RatePlan ratePlan = parseRatePlan(request.ratePlan());

        Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                checkIn,
                checkOut,
                request.guests(),
                ratePlan,
                request.companyCode()
        );

        ctx.status(201).json(ReservationDto.from(reservation));
    }

    private void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.findReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    private void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    private void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    private void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationDto.from(reservation));
    }

    private void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        DocumentDto document = createDocumentDto(reservation);
        ctx.json(document);
    }

    private void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.findReservation(id);
        DocumentDto document = createDocumentDto(reservation);
        ctx.json(document);
    }

    private void getMessages(Context ctx) {
        List<MessageDto> messages = messageRepository.findAll().stream()
                .map(MessageDto::from)
                .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new ReservationService.InvalidRequestException("Date parameter is required");
        }
        LocalDate date = parseDate(dateStr);
        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new ReservationService.InvalidRequestException("Month parameter is required");
        }
        YearMonth month = parseMonth(monthStr);
        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private DocumentDto createDocumentDto(Reservation reservation) {
        String roomType = roomRepository.findByNumber(reservation.getRoomNumber())
                .map(r -> r.getType().name())
                .orElse("");

        return new DocumentDto(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                roomType,
                reservation.getCheckIn().toString(),
                reservation.getCheckOut().toString(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new ReservationService.InvalidRequestException(
                    "Field '" + fieldName + "' is required");
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new ReservationService.InvalidRequestException("Invalid date format");
        }
    }

    private YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            throw new ReservationService.InvalidRequestException("Invalid month format");
        }
    }

    private RatePlan parseRatePlan(String ratePlanStr) {
        try {
            return RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw new ReservationService.InvalidRequestException("Invalid rate plan");
        }
    }
}
