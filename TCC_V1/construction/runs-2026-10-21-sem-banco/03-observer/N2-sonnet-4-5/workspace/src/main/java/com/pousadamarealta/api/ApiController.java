package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.GuestNotifier;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class ApiController {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final GuestNotifier guestNotifier;

    public ApiController(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            MessageRepository messageRepository,
            ReservationService reservationService,
            ReportService reportService,
            GuestNotifier guestNotifier) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.guestNotifier = guestNotifier;
    }

    public Javalin createApp() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, true));
        });

        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkInReservation);
        app.post("/reservations/{id}/check-out", this::checkOutReservation);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) ->
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage())));
        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) ->
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage())));
        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) ->
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage())));
        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) ->
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage())));
        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) ->
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage())));
        app.exception(IllegalStateException.class, (e, ctx) ->
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage())));
        app.exception(DateTimeParseException.class, (e, ctx) ->
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format")));

        return app;
    }

    private void getRooms(Context ctx) {
        List<RoomDto> rooms = roomRepository.findAll().stream()
            .map(r -> new RoomDto(r.number(), r.type(), r.capacity(), r.nightlyRate()))
            .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        CreateReservationDto dto = ctx.bodyAsClass(CreateReservationDto.class);

        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            dto.roomNumber,
            dto.guestName,
            dto.guestEmail,
            dto.guestCpf,
            dto.checkIn,
            dto.checkOut,
            dto.guests,
            dto.ratePlan
        );

        Reservation reservation = reservationService.create(request);
        ctx.status(201).json(toDto(reservation));
    }

    private void getReservation(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = reservationService.findById(id);
        ctx.json(toDto(reservation));
    }

    private void confirmReservation(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = reservationService.confirm(id);
        ctx.json(toDto(reservation));
    }

    private void cancelReservation(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = reservationService.cancel(id);
        ctx.json(toDto(reservation));
    }

    private void checkInReservation(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toDto(reservation));
    }

    private void checkOutReservation(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(buildReceipt(reservation));
    }

    private void getDocument(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = reservationService.findById(id);

        if (!reservation.isPaid()) {
            throw new IllegalStateException("Document is only available for paid reservations");
        }

        ctx.json(buildReceipt(reservation));
    }

    private void getMessages(Context ctx) {
        List<MessageDto> messages = messageRepository.findAll().stream()
            .map(m -> new MessageDto(m.id(), m.channel(), m.to(), m.subject(), m.body()))
            .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new ReservationService.InvalidRequestException("Missing date parameter");
        }

        LocalDate date = LocalDate.parse(dateStr);
        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new ReservationService.InvalidRequestException("Missing month parameter");
        }

        YearMonth month = YearMonth.parse(monthStr);
        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private ReservationDto toDto(Reservation r) {
        return new ReservationDto(
            r.id(),
            r.roomNumber(),
            r.guestName(),
            r.guestEmail(),
            r.guestCpf(),
            r.checkIn(),
            r.checkOut(),
            r.guests(),
            r.ratePlan(),
            r.nights(),
            r.subtotal(),
            r.discount(),
            r.total(),
            r.status(),
            r.amountPaid(),
            r.refund()
        );
    }

    private DocumentDto buildReceipt(Reservation r) {
        return new DocumentDto(
            "Comprovante de reserva",
            r.id(),
            r.guestName(),
            r.guestCpf(),
            r.roomNumber(),
            r.roomType(),
            r.checkIn(),
            r.checkOut(),
            r.nights(),
            r.subtotal(),
            r.discount(),
            r.total()
        );
    }

    record RoomDto(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

    record CreateReservationDto(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {}

    record ReservationDto(
        int id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund
    ) {}

    record DocumentDto(
        String title,
        int reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    ) {}

    record MessageDto(
        int id,
        MessageChannel channel,
        String to,
        String subject,
        String body
    ) {}
}
