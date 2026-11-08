package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {

    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ReservationController(ReservationService reservationService,
                                RoomRepository roomRepository,
                                NotificationService notificationService,
                                ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public void createReservation(Context ctx) {
        Map<String, Object> request = ctx.bodyAsClass(Map.class);

        String roomNumber = getRequiredString(request, "roomNumber");
        String guestName = getRequiredString(request, "guestName");
        String guestEmail = getRequiredString(request, "guestEmail");
        String guestCpf = getRequiredString(request, "guestCpf");
        LocalDate checkIn = getRequiredDate(request, "checkIn");
        LocalDate checkOut = getRequiredDate(request, "checkOut");
        Integer guests = getRequiredInteger(request, "guests");
        RatePlan ratePlan = getRequiredRatePlan(request, "ratePlan");
        String companyCode = (String) request.get("companyCode");

        Reservation reservation = reservationService.createReservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, companyCode
        );

        ctx.status(201).json(toReservationResponse(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String receiptText = notificationService.generateReceipt(reservation, room.getType().name());

        ctx.json(toDocumentResponse(reservation, receiptText, room.getType().name()));
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getStatus() != com.pousadamarealta.domain.ReservationStatus.CONFIRMED &&
            reservation.getStatus() != com.pousadamarealta.domain.ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != com.pousadamarealta.domain.ReservationStatus.CHECKED_OUT) {
            throw new InvalidRequestException("Document only available for confirmed reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String receiptText = notificationService.generateReceipt(reservation, room.getType().name());

        ctx.json(toDocumentResponse(reservation, receiptText, room.getType().name()));
    }

    private Map<String, Object> toReservationResponse(Reservation reservation) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", reservation.getId());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("guestName", reservation.getGuestName());
        response.put("guestEmail", reservation.getGuestEmail());
        response.put("guestCpf", reservation.getGuestCpf());
        response.put("checkIn", reservation.getCheckIn().toString());
        response.put("checkOut", reservation.getCheckOut().toString());
        response.put("guests", reservation.getGuests());
        response.put("ratePlan", reservation.getRatePlan().name());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        response.put("status", reservation.getStatus().name());
        response.put("amountPaid", reservation.getAmountPaid());
        response.put("refund", reservation.getRefund());
        return response;
    }

    private Map<String, Object> toDocumentResponse(Reservation reservation, String receiptText, String roomType) {
        String[] lines = receiptText.split("\n");

        Map<String, Object> response = new HashMap<>();
        response.put("title", "Comprovante de reserva");
        response.put("reservationId", reservation.getId());
        response.put("guestName", reservation.getGuestName());
        response.put("guestCpf", reservation.getGuestCpf());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("roomType", roomType);
        response.put("checkIn", reservation.getCheckIn().toString());
        response.put("checkOut", reservation.getCheckOut().toString());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        return response;
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || !(value instanceof String) || ((String) value).isBlank()) {
            throw new InvalidRequestException("Missing or invalid field: " + key);
        }
        return (String) value;
    }

    private Integer getRequiredInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new InvalidRequestException("Missing field: " + key);
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new InvalidRequestException("Invalid field type: " + key);
    }

    private LocalDate getRequiredDate(Map<String, Object> map, String key) {
        String value = getRequiredString(map, key);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format for " + key + ": " + value);
        }
    }

    private RatePlan getRequiredRatePlan(Map<String, Object> map, String key) {
        String value = getRequiredString(map, key);
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan: " + value);
        }
    }
}
