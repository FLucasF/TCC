package com.pousada.api;

import com.google.gson.Gson;
import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.ErrorResponse;
import com.pousada.models.Message;
import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.MessageRepository;
import com.pousada.repositories.ReservationRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.services.MessageService;
import com.pousada.services.ReceiptService;
import com.pousada.services.ReportService;
import com.pousada.services.ReservationService;
import com.pousada.utils.FormatterUtil;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ApiController {
    private final Gson gson;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReceiptService receiptService;
    private final ReportService reportService;

    public ApiController(Javalin app, Gson gson, RoomRepository roomRepository,
                        ReservationRepository reservationRepository, MessageRepository messageRepository,
                        ReservationService reservationService, MessageService messageService,
                        ReceiptService receiptService, ReportService reportService) {
        this.gson = gson;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.receiptService = receiptService;
        this.reportService = reportService;

        setupRoutes(app);
    }

    private void setupRoutes(Javalin app) {
        app.get("/rooms", this::listRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::listMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void listRooms(io.javalin.http.Context ctx) throws Exception {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms.stream().map(ApiMapper::mapRoom).toList());
    }

    private void createReservation(io.javalin.http.Context ctx) throws Exception {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            if (req.getRoomNumber() == null || req.getGuestName() == null || req.getGuestEmail() == null ||
                req.getGuestCpf() == null || req.getCheckIn() == null || req.getCheckOut() == null ||
                req.getGuests() == null || req.getRatePlan() == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            Room room = roomRepository.findByNumber(req.getRoomNumber());
            if (room == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            LocalDate checkIn = FormatterUtil.parseDate(req.getCheckIn());
            LocalDate checkOut = FormatterUtil.parseDate(req.getCheckOut());

            if (!checkIn.isBefore(checkOut)) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (req.getGuests() < 1) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "At least one guest required"));
                return;
            }

            if (req.getGuests() > room.getCapacity()) {
                ctx.status(422);
                ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Room capacity exceeded"));
                return;
            }

            if (!reservationRepository.isRoomAvailable(req.getRoomNumber(), checkIn, checkOut)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                req.getRoomNumber(),
                req.getGuestName(),
                req.getGuestEmail(),
                req.getGuestCpf(),
                checkIn,
                checkOut,
                req.getGuests(),
                req.getRatePlan()
            );

            ctx.status(201);
            ctx.json(ApiMapper.mapReservation(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void getReservation(io.javalin.http.Context ctx) throws Exception {
        Long id = Long.valueOf(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id);

        if (reservation == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        ctx.json(ApiMapper.mapReservation(reservation));
    }

    private void confirmReservation(io.javalin.http.Context ctx) throws Exception {
        try {
            Long id = Long.valueOf(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!"PENDING".equals(reservation.getStatus())) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm a non-pending reservation"));
                return;
            }

            BigDecimal amountPaid = reservation.getTotal();
            Reservation confirmed = reservationService.confirmReservation(id, amountPaid);

            Receipt receipt = receiptService.generateReceipt(confirmed);
            String receiptText = receiptService.formatReceipt(receipt);

            messageService.sendConfirmationEmail(confirmed.getGuestName(), confirmed.getGuestEmail(), receiptText);

            ctx.json(ApiMapper.mapReservation(confirmed));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void checkIn(io.javalin.http.Context ctx) throws Exception {
        try {
            Long id = Long.valueOf(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!"CONFIRMED".equals(reservation.getStatus())) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check in a non-confirmed reservation"));
                return;
            }

            Reservation checkedIn = reservationService.checkIn(id);
            ctx.json(ApiMapper.mapReservation(checkedIn));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void checkOut(io.javalin.http.Context ctx) throws Exception {
        try {
            Long id = Long.valueOf(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!"CHECKED_IN".equals(reservation.getStatus())) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check out a non-checked-in reservation"));
                return;
            }

            Reservation checkedOut = reservationService.checkOut(id);
            Receipt receipt = receiptService.generateReceipt(checkedOut);

            ctx.json(ApiMapper.mapReceipt(receipt));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) throws Exception {
        try {
            Long id = Long.valueOf(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            String status = reservation.getStatus();
            if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel a reservation in this status"));
                return;
            }

            Reservation cancelled = reservationService.cancelReservation(id);
            ctx.json(ApiMapper.mapReservation(cancelled));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void getDocument(io.javalin.http.Context ctx) throws Exception {
        Long id = Long.valueOf(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id);

        if (reservation == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        if (!"CONFIRMED".equals(reservation.getStatus()) &&
            !"CHECKED_IN".equals(reservation.getStatus()) &&
            !"CHECKED_OUT".equals(reservation.getStatus())) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for confirmed reservations"));
            return;
        }

        Receipt receipt = receiptService.generateReceipt(reservation);
        ctx.json(ApiMapper.mapReceipt(receipt));
    }

    private void listMessages(io.javalin.http.Context ctx) throws Exception {
        List<Message> messages = messageRepository.findAll();
        ctx.json(messages.stream().map(ApiMapper::mapMessage).toList());
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) throws Exception {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
            return;
        }

        try {
            LocalDate date = FormatterUtil.parseDate(dateParam);
            String report = reportService.getOccupancyReport(date);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) throws Exception {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
            return;
        }

        try {
            YearMonth ym = YearMonth.parse(monthParam);
            String report = reportService.getRevenueReport(ym.getYear(), ym.getMonthValue());
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format (use YYYY-MM)"));
        }
    }
}
