package com.pousadamarealta.api;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ApiController {
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final MessageRepository messageRepository;
    private final ReportService reportService;

    public ApiController(RoomRepository roomRepository,
                        ReservationService reservationService,
                        MessageRepository messageRepository,
                        ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
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
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });
        app.exception(DateTimeParseException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        });
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms.stream().map(RoomResponse::from).toList());
    }

    private void createReservation(Context ctx) {
        try {
            ReservationRequest request = ctx.bodyAsClass(ReservationRequest.class);

            if (request.roomNumber() == null || request.guestName() == null ||
                request.guestEmail() == null || request.guestCpf() == null ||
                request.checkIn() == null || request.checkOut() == null ||
                request.guests() == null || request.ratePlan() == null) {
                throw new InvalidRequestException("Missing required fields");
            }

            Reservation reservation = new Reservation();
            reservation.setRoomNumber(request.roomNumber());
            reservation.setGuestName(request.guestName());
            reservation.setGuestEmail(request.guestEmail());
            reservation.setGuestCpf(request.guestCpf());
            reservation.setCheckIn(request.checkIn());
            reservation.setCheckOut(request.checkOut());
            reservation.setGuests(request.guests());
            reservation.setRatePlan(request.ratePlan());
            reservation.setCompanyCode(request.companyCode());

            Reservation created = reservationService.createReservation(reservation);
            ctx.status(201).json(ReservationResponse.from(created));
        } catch (Exception e) {
            if (e instanceof InvalidRequestException ||
                e instanceof RoomNotFoundException ||
                e instanceof RoomUnavailableException ||
                e instanceof CapacityExceededException ||
                e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new InvalidRequestException("Invalid request: " + e.getMessage());
        }
    }

    private void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    private void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(DocumentResponse.from(reservation));
    }

    private void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        ctx.json(DocumentResponse.from(reservation));
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        ctx.json(messages.stream().map(MessageResponse::from).toList());
    }

    private void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new InvalidRequestException("Missing date parameter");
        }

        LocalDate date = LocalDate.parse(dateParam);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new InvalidRequestException("Missing month parameter");
        }

        YearMonth month = YearMonth.parse(monthParam);
        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
