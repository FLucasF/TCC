package com.marealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marealta.api.dto.CreateReservationRequest;
import com.marealta.api.dto.ErrorResponse;
import com.marealta.domain.model.*;
import com.marealta.domain.service.*;
import com.marealta.infrastructure.database.ReservationRepository;
import com.marealta.infrastructure.database.RoomRepository;
import com.marealta.infrastructure.messaging.MessageRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiController {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;
    private final EmailService emailService;
    private final ReportService reportService;
    private final ValidationService validationService;
    private final ObjectMapper objectMapper;

    public ApiController(
        RoomRepository roomRepository,
        ReservationRepository reservationRepository,
        MessageRepository messageRepository
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.pricingService = new PricingService();
        this.documentService = new DocumentService();
        this.emailService = new EmailService(messageRepository);
        this.reportService = new ReportService();
        this.validationService = new ValidationService();
        this.objectMapper = JsonMapper.get();
    }

    public void registerRoutes(Javalin app) {
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

        app.exception(ValidationService.ValidationException.class, (e, ctx) -> {
            int status = switch (e.getErrorCode()) {
                case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
                case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
                case "CAPACITY_EXCEEDED" -> 422;
                default -> 400;
            };
            ctx.status(status).json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });
    }

    private void getRooms(Context ctx) throws Exception {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .<Map<String, Object>>map(room -> Map.of(
                "number", room.number(),
                "type", room.type().toString(),
                "capacity", room.capacity(),
                "nightlyRate", room.nightlyRate()
            ))
            .toList();
        ctx.json(response);
    }

    private void createReservation(Context ctx) throws Exception {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
        validationService.validateCreateReservation(request);

        Room room = roomRepository.findByNumber(request.roomNumber())
            .orElseThrow(() -> new ValidationService.ValidationException("ROOM_NOT_FOUND",
                "Room " + request.roomNumber() + " not found"));

        validationService.validateCapacity(room, request.guests());

        if (reservationRepository.hasConflictingReservation(
                request.roomNumber(), request.checkIn(), request.checkOut(), null)) {
            throw new ValidationService.ValidationException("ROOM_UNAVAILABLE",
                "Room is not available for the selected dates");
        }

        GuestIdentification guest = createGuestIdentification(request);
        RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

        PricingService.PricingResult pricing = pricingService.calculate(
            room, request.checkIn(), request.checkOut(), ratePlan);

        Reservation reservation = new Reservation(
            null,
            room.number(),
            guest,
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            ratePlan,
            pricing.nights,
            pricing.subtotal,
            pricing.discount,
            pricing.total
        );

        reservationRepository.save(reservation);

        ctx.status(201).json(toReservationResponse(reservation));
    }

    private void getReservation(Context ctx) throws Exception {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationService.ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));

        ctx.json(toReservationResponse(reservation));
    }

    private void confirmReservation(Context ctx) throws Exception {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationService.ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));

        reservation.confirm();
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = documentService.generate(reservation, room);
        emailService.sendConfirmationEmail(
            reservation.getGuest().getGuestName(),
            reservation.getGuest().getGuestEmail(),
            document
        );

        ctx.json(toReservationResponse(reservation));
    }

    private void cancelReservation(Context ctx) throws Exception {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationService.ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));

        reservation.cancel();
        reservationRepository.update(reservation);

        ctx.json(toReservationResponse(reservation));
    }

    private void checkInReservation(Context ctx) throws Exception {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationService.ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));

        reservation.checkIn();
        reservationRepository.update(reservation);

        ctx.json(toReservationResponse(reservation));
    }

    private void checkOutReservation(Context ctx) throws Exception {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationService.ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));

        reservation.checkOut();
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = documentService.generate(reservation, room);

        ctx.json(toDocumentResponse(document));
    }

    private void getDocument(Context ctx) throws Exception {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationService.ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));

        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationService.ValidationException("OPERATION_NOT_ALLOWED",
                "Document is only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = documentService.generate(reservation, room);

        ctx.json(toDocumentResponse(document));
    }

    private void getMessages(Context ctx) throws Exception {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .<Map<String, Object>>map(msg -> Map.of(
                "id", msg.getId(),
                "channel", msg.getChannel(),
                "to", msg.getTo(),
                "subject", msg.getSubject(),
                "body", msg.getBody()
            ))
            .toList();
        ctx.json(response);
    }

    private void getOccupancyReport(Context ctx) throws Exception {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new ValidationService.ValidationException("INVALID_REQUEST", "Date parameter is required");
        }

        LocalDate date = validationService.parseDate(dateParam);
        List<Reservation> reservations = reservationRepository.findAll();
        List<Room> rooms = roomRepository.findAll();

        String csv = reportService.generateOccupancyReport(date, reservations, rooms);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) throws Exception {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new ValidationService.ValidationException("INVALID_REQUEST", "Month parameter is required");
        }

        YearMonth month = validationService.parseMonth(monthParam);
        List<Reservation> reservations = reservationRepository.findAll();

        String csv = reportService.generateRevenueReport(month, reservations);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private GuestIdentification createGuestIdentification(CreateReservationRequest request) {
        if (request.guestCpf() != null) {
            return new CpfGuest(request.guestName(), request.guestEmail(), request.guestCpf());
        } else if (request.companyCnpj() != null) {
            return new CompanyGuest(request.guestName(), request.guestEmail(),
                request.companyCnpj(), request.companyName());
        } else if (request.guestPassport() != null) {
            return new PassportGuest(request.guestName(), request.guestEmail(),
                request.guestPassport(), request.guestCountry());
        }
        throw new ValidationService.ValidationException("INVALID_REQUEST", "Invalid guest identification");
    }

    private Map<String, Object> toReservationResponse(Reservation reservation) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", reservation.getId());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("guestName", reservation.getGuest().getGuestName());
        response.put("guestEmail", reservation.getGuest().getGuestEmail());

        GuestIdentification guest = reservation.getGuest();
        response.put("guestCpf", guest instanceof CpfGuest cpf ? cpf.guestCpf() : null);
        response.put("companyCnpj", guest instanceof CompanyGuest company ? company.companyCnpj() : null);
        response.put("companyName", guest instanceof CompanyGuest company ? company.companyName() : null);
        response.put("guestPassport", guest instanceof PassportGuest passport ? passport.guestPassport() : null);
        response.put("guestCountry", guest instanceof PassportGuest passport ? passport.guestCountry() : null);

        response.put("checkIn", reservation.getCheckIn());
        response.put("checkOut", reservation.getCheckOut());
        response.put("guests", reservation.getGuests());
        response.put("ratePlan", reservation.getRatePlan().toString());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        response.put("status", reservation.getStatus().toString());
        response.put("amountPaid", reservation.getAmountPaid());
        response.put("refund", reservation.getRefund());

        return response;
    }

    private Map<String, Object> toDocumentResponse(Document document) {
        Map<String, Object> response = new HashMap<>();
        response.put("title", getDocumentTitle(document));
        response.put("text", document.text());
        response.put("reservationId", document.reservationId());
        response.put("guestName", document.guestName());
        response.put("guestCpf", document.guestCpf());
        response.put("companyCnpj", document.companyCnpj());
        response.put("companyName", document.companyName());
        response.put("guestPassport", document.guestPassport());
        response.put("guestCountry", document.guestCountry());
        response.put("roomNumber", document.roomNumber());
        response.put("roomType", document.roomType().toString());
        response.put("checkIn", document.checkIn());
        response.put("checkOut", document.checkOut());
        response.put("nights", document.nights());
        response.put("subtotal", document.subtotal());
        response.put("discount", document.discount());
        response.put("total", document.total());
        return response;
    }

    private String getDocumentTitle(Document document) {
        if (document.guestCpf() != null) {
            return "Comprovante de reserva";
        } else if (document.companyCnpj() != null) {
            return "Fatura de hospedagem";
        } else {
            return "Booking receipt";
        }
    }
}
