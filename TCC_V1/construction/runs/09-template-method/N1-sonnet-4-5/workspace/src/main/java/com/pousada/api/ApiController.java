package com.pousada.api;

import com.pousada.api.dto.*;
import com.pousada.domain.*;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ApiController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;

    public ApiController(RoomService roomService, ReservationService reservationService,
                        MessageService messageService, ReportService reportService) {
        this.roomService = roomService;
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
        app.get("/reports/tourism-tax", this::getTourismTaxReport);
        app.get("/reports/rate-plans", this::getRatePlansReport);

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

        app.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });
    }

    private void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
                .map(RoomResponse::new)
                .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateCreateRequest(request);

            LocalDate checkIn = LocalDate.parse(request.getCheckIn());
            LocalDate checkOut = LocalDate.parse(request.getCheckOut());
            RatePlan ratePlan = RatePlan.valueOf(request.getRatePlan());

            if (!checkOut.isAfter(checkIn)) {
                throw new ValidationException("Check-out date must be after check-in date");
            }

            if (request.getGuests() < 1) {
                throw new ValidationException("Number of guests must be at least 1");
            }

            Reservation reservation = reservationService.createReservation(
                    request.getRoomNumber(),
                    request.getGuestName(),
                    request.getGuestEmail(),
                    request.getGuestCpf(),
                    checkIn,
                    checkOut,
                    request.getGuests(),
                    ratePlan
            );

            ctx.status(201).json(new ReservationResponse(reservation));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected: yyyy-MM-dd");
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid rate plan. Expected: STANDARD or NON_REFUNDABLE");
        }
    }

    private void validateCreateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getRoomNumber().isEmpty()) {
            throw new ValidationException("Room number is required");
        }
        if (request.getGuestName() == null || request.getGuestName().isEmpty()) {
            throw new ValidationException("Guest name is required");
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().isEmpty()) {
            throw new ValidationException("Guest email is required");
        }
        if (request.getGuestCpf() == null || request.getGuestCpf().isEmpty()) {
            throw new ValidationException("Guest CPF is required");
        }
        if (request.getCheckIn() == null || request.getCheckIn().isEmpty()) {
            throw new ValidationException("Check-in date is required");
        }
        if (request.getCheckOut() == null || request.getCheckOut().isEmpty()) {
            throw new ValidationException("Check-out date is required");
        }
        if (request.getGuests() == null) {
            throw new ValidationException("Number of guests is required");
        }
        if (request.getRatePlan() == null || request.getRatePlan().isEmpty()) {
            throw new ValidationException("Rate plan is required");
        }
    }

    private void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        DocumentResponse document = createDocument(reservation);
        ctx.json(document);
    }

    private void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        DocumentResponse document = createDocument(reservation);
        ctx.json(document);
    }

    private DocumentResponse createDocument(Reservation reservation) {
        DocumentResponse doc = new DocumentResponse();
        doc.setTitle("Comprovante de reserva");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(reservation.getRoomType().name());
        doc.setCheckIn(reservation.getCheckIn().toString());
        doc.setCheckOut(reservation.getCheckOut().toString());
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());
        return doc;
    }

    private void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
                .map(MessageResponse::new)
                .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null || dateStr.isEmpty()) {
            throw new ValidationException("Date parameter is required");
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isEmpty()) {
            throw new ValidationException("Month parameter is required");
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid month format. Expected: yyyy-MM");
        }
    }

    private void getTourismTaxReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isEmpty()) {
            throw new ValidationException("Month parameter is required");
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateTourismTaxReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid month format. Expected: yyyy-MM");
        }
    }

    private void getRatePlansReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isEmpty()) {
            throw new ValidationException("Month parameter is required");
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRatePlansReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid month format. Expected: yyyy-MM");
        }
    }
}
