package com.pousada.controller;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.exception.BusinessException;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {

    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);

        validateRequiredField(body, "roomNumber");
        validateRequiredField(body, "guestName");
        validateRequiredField(body, "guestEmail");
        validateRequiredField(body, "guestCpf");
        validateRequiredField(body, "checkIn");
        validateRequiredField(body, "checkOut");
        validateRequiredField(body, "guests");
        validateRequiredField(body, "ratePlan");

        String roomNumber = (String) body.get("roomNumber");
        String guestName = (String) body.get("guestName");
        String guestEmail = (String) body.get("guestEmail");
        String guestCpf = (String) body.get("guestCpf");
        String companyCode = (String) body.get("companyCode");

        LocalDate checkIn;
        LocalDate checkOut;

        try {
            checkIn = LocalDate.parse((String) body.get("checkIn"));
            checkOut = LocalDate.parse((String) body.get("checkOut"));
        } catch (DateTimeParseException e) {
            throw new BusinessException("INVALID_REQUEST", "Invalid date format. Use yyyy-MM-dd", 400);
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("INVALID_REQUEST",
                "Check-out date must be after check-in date", 400);
        }

        int guests;
        try {
            guests = ((Number) body.get("guests")).intValue();
        } catch (Exception e) {
            throw new BusinessException("INVALID_REQUEST", "Invalid guests value", 400);
        }

        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf((String) body.get("ratePlan"));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_REQUEST", "Invalid rate plan", 400);
        }

        Reservation reservation = reservationService.createReservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, companyCode
        );

        ctx.status(201);
        ctx.json(toReservationDto(reservation));
    }

    public void getReservation(Context ctx) {
        long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    public void checkIn(Context ctx) {
        long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationDto(reservation));
    }

    public void checkOut(Context ctx) {
        long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        String document = reservationService.generateDocument(id);
        ctx.json(toDocumentDto(reservation, document));
    }

    public void cancelReservation(Context ctx) {
        long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    public void getDocument(Context ctx) {
        long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        String document = reservationService.generateDocument(id);
        ctx.json(toDocumentDto(reservation, document));
    }

    private void validateRequiredField(Map<String, Object> body, String field) {
        if (!body.containsKey(field) || body.get(field) == null) {
            throw new BusinessException("INVALID_REQUEST",
                "Missing required field: " + field, 400);
        }
    }

    private long parseId(String idParam) {
        try {
            return Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            throw new BusinessException("INVALID_REQUEST", "Invalid reservation ID", 400);
        }
    }

    private Map<String, Object> toReservationDto(Reservation r) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", r.getId());
        dto.put("roomNumber", r.getRoomNumber());
        dto.put("guestName", r.getGuestName());
        dto.put("guestEmail", r.getGuestEmail());
        dto.put("guestCpf", r.getGuestCpf());
        dto.put("checkIn", r.getCheckIn().toString());
        dto.put("checkOut", r.getCheckOut().toString());
        dto.put("guests", r.getGuests());
        dto.put("ratePlan", r.getRatePlan().toString());
        dto.put("nights", r.getNights());
        dto.put("subtotal", r.getSubtotal());
        dto.put("discount", r.getDiscount());
        dto.put("total", r.getTotal());
        dto.put("status", r.getStatus().toString());
        dto.put("amountPaid", r.getAmountPaid());
        dto.put("refund", r.getRefund());
        return dto;
    }

    private Map<String, Object> toDocumentDto(Reservation r, String documentText) {
        Room room = roomService.getRoom(r.getRoomNumber());
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", "Comprovante de reserva");
        dto.put("reservationId", r.getId());
        dto.put("guestName", r.getGuestName());
        dto.put("guestCpf", r.getGuestCpf());
        dto.put("roomNumber", r.getRoomNumber());
        dto.put("roomType", room.getType().toString());
        dto.put("checkIn", r.getCheckIn().toString());
        dto.put("checkOut", r.getCheckOut().toString());
        dto.put("nights", r.getNights());
        dto.put("subtotal", r.getSubtotal());
        dto.put("discount", r.getDiscount());
        dto.put("total", r.getTotal());
        return dto;
    }
}
