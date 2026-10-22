package com.pousada.maralta.controller;

import com.pousada.maralta.controller.dto.*;
import com.pousada.maralta.domain.*;
import com.pousada.maralta.repository.MessageRepository;
import com.pousada.maralta.repository.ReservationRepository;
import com.pousada.maralta.repository.RoomRepository;
import com.pousada.maralta.service.ReportService;
import com.pousada.maralta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.pousada.maralta.service.ReservationService.*;

public class ApiController {
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public ApiController(ReservationService reservationService,
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

        app.exception(RoomNotFoundException.class, (e, ctx) ->
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found")));
        app.exception(ReservationNotFoundException.class, (e, ctx) ->
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found")));
        app.exception(RoomUnavailableException.class, (e, ctx) ->
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates")));
        app.exception(OperationNotAllowedException.class, (e, ctx) ->
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status")));
        app.exception(CapacityExceededException.class, (e, ctx) ->
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity")));
        app.exception(InvalidRequestException.class, (e, ctx) ->
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage())));
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms.stream().map(RoomResponse::from).toList());
    }

    private void createReservation(Context ctx) {
        CreateReservationRequest request;
        try {
            request = ctx.bodyAsClass(CreateReservationRequest.class);
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid JSON body");
        }

        validateRequest(request);

        GuestIdentification identification = parseIdentification(request);
        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf(request.ratePlan());
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid rate plan");
        }

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            identification,
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            ratePlan
        );

        ctx.status(201).json(ReservationResponse.from(reservation));
    }

    private void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReservationDocument document = reservationService.checkOut(id);
        ctx.json(DocumentResponse.from(document));
    }

    private void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReservationDocument document = reservationService.getDocument(id);
        ctx.json(DocumentResponse.from(document));
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        ctx.json(messages.stream().map(MessageResponse::from).toList());
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new InvalidRequestException("Missing date parameter");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }

        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.checkIn() == null ||
            request.checkOut() == null || request.guests() == null ||
            request.ratePlan() == null) {
            throw new InvalidRequestException("Missing required fields");
        }

        if (request.checkOut().isBefore(request.checkIn()) ||
            request.checkOut().equals(request.checkIn())) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (request.guests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }
    }

    private GuestIdentification parseIdentification(CreateReservationRequest request) {
        boolean hasCpf = request.guestCpf() != null;
        boolean hasCnpj = request.companyCnpj() != null || request.companyName() != null;
        boolean hasPassport = request.guestPassport() != null || request.guestCountry() != null;

        int count = (hasCpf ? 1 : 0) + (hasCnpj ? 1 : 0) + (hasPassport ? 1 : 0);

        if (count != 1) {
            throw new InvalidRequestException(
                "Exactly one identification method required: guestCpf, or (companyCnpj + companyName), or (guestPassport + guestCountry)"
            );
        }

        if (hasCpf) {
            return new CpfIdentification(request.guestCpf());
        } else if (hasCnpj) {
            if (request.companyCnpj() == null || request.companyName() == null) {
                throw new InvalidRequestException("Both companyCnpj and companyName are required");
            }
            return new CnpjIdentification(request.companyCnpj(), request.companyName());
        } else {
            if (request.guestPassport() == null || request.guestCountry() == null) {
                throw new InvalidRequestException("Both guestPassport and guestCountry are required");
            }
            return new PassportIdentification(request.guestPassport(), request.guestCountry());
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
