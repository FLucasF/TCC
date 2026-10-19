package com.pousadamarealta.controller;

import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.model.ApiException;
import com.pousadamarealta.model.Document;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
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
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        LocalDate checkIn = parseDate(request.checkIn);
        LocalDate checkOut = parseDate(request.checkOut);

        Reservation reservation = reservationService.createReservation(
            request.roomNumber,
            request.guestName,
            request.guestEmail,
            request.guestCpf,
            checkIn,
            checkOut,
            request.guests,
            request.ratePlan
        );

        ctx.status(201).json(toReservationResponse(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(toDocumentResponse(document));
    }

    public void getDocument(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentResponse(document));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber == null || request.roomNumber.isEmpty()) {
            throw ApiException.invalidRequest("Campo roomNumber é obrigatório");
        }
        if (request.guestName == null || request.guestName.isEmpty()) {
            throw ApiException.invalidRequest("Campo guestName é obrigatório");
        }
        if (request.guestEmail == null || request.guestEmail.isEmpty()) {
            throw ApiException.invalidRequest("Campo guestEmail é obrigatório");
        }
        if (request.guestCpf == null || request.guestCpf.isEmpty()) {
            throw ApiException.invalidRequest("Campo guestCpf é obrigatório");
        }
        if (request.checkIn == null || request.checkIn.isEmpty()) {
            throw ApiException.invalidRequest("Campo checkIn é obrigatório");
        }
        if (request.checkOut == null || request.checkOut.isEmpty()) {
            throw ApiException.invalidRequest("Campo checkOut é obrigatório");
        }
        if (request.guests == null) {
            throw ApiException.invalidRequest("Campo guests é obrigatório");
        }
        if (request.ratePlan == null) {
            throw ApiException.invalidRequest("Campo ratePlan é obrigatório");
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Formato de data inválido. Use yyyy-MM-dd");
        }
    }

    private Long parseId(String idStr) {
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound();
        }
    }

    private Map<String, Object> toReservationResponse(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn());
        map.put("checkOut", reservation.getCheckOut());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentResponse(Document document) {
        Room room = roomService.getRoom(document.getRoomNumber());

        Map<String, Object> map = new HashMap<>();
        map.put("title", document.getTitle());
        map.put("reservationId", document.getReservationId());
        map.put("guestName", document.getGuestName());
        map.put("guestCpf", document.getGuestCpf());
        map.put("roomNumber", document.getRoomNumber());
        map.put("roomType", document.getRoomType());
        map.put("checkIn", document.getCheckIn());
        map.put("checkOut", document.getCheckOut());
        map.put("nights", document.getNights());
        map.put("subtotal", document.getSubtotal());
        map.put("discount", document.getDiscount());
        map.put("total", document.getTotal());
        return map;
    }
}
