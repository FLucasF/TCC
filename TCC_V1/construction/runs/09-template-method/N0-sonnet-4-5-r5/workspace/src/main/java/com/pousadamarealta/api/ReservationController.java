package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public ReservationController(ReservationService reservationService,
                                ReportService reportService,
                                RoomRepository roomRepository,
                                MessageRepository messageRepository) {
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .map(room -> {
                Map<String, Object> map = new HashMap<>();
                map.put("number", room.getNumber());
                map.put("type", room.getType().name());
                map.put("capacity", room.getCapacity());
                map.put("nightlyRate", room.getNightlyRate());
                return map;
            })
            .toList();
        ctx.json(response);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequiredField(request.getRoomNumber(), "roomNumber");
            validateRequiredField(request.getGuestName(), "guestName");
            validateRequiredField(request.getGuestEmail(), "guestEmail");
            validateRequiredField(request.getGuestCpf(), "guestCpf");
            validateRequiredField(request.getCheckIn(), "checkIn");
            validateRequiredField(request.getCheckOut(), "checkOut");
            validateRequiredField(request.getGuests(), "guests");
            validateRequiredField(request.getRatePlan(), "ratePlan");

            LocalDate checkIn = parseDate(request.getCheckIn());
            LocalDate checkOut = parseDate(request.getCheckOut());
            RatePlan ratePlan = parseRatePlan(request.getRatePlan());

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

            ctx.status(201).json(toMap(reservation));
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toMap(reservation));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            String document = reservationService.generateDocument(reservation);
            ctx.json(toDocumentMap(reservation, document));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            String document = reservationService.generateDocument(reservation);
            ctx.json(toDocumentMap(reservation, document));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                throw new InvalidRequestException("date parameter is required");
            }
            LocalDate date = parseDate(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                throw new InvalidRequestException("month parameter is required");
            }
            YearMonth month = parseMonth(monthParam);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                throw new InvalidRequestException("month parameter is required");
            }
            YearMonth month = parseMonth(monthParam);
            String csv = reportService.generateTourismTaxReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRatePlansReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                throw new InvalidRequestException("month parameter is required");
            }
            YearMonth month = parseMonth(monthParam);
            String csv = reportService.generateRatePlansReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void validateRequiredField(Object value, String fieldName) {
        if (value == null) {
            throw new InvalidRequestException("Missing required field: " + fieldName);
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            throw new InvalidRequestException("Missing required field: " + fieldName);
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + date);
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format: " + month);
        }
    }

    private RatePlan parseRatePlan(String ratePlan) {
        try {
            return RatePlan.valueOf(ratePlan);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan: " + ratePlan);
        }
    }

    private Map<String, Object> toMap(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentMap(Reservation reservation, String document) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", room.getType().name());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }
}
