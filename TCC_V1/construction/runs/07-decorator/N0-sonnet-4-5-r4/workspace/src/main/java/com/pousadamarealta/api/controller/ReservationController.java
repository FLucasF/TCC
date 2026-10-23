package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

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

        ctx.status(201).json(toResponse(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toResponse(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toResponse(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toResponse(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Map<String, Object> document = reservationService.checkOut(id);
        ctx.json(document);
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toResponse(reservation));
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Map<String, Object> document = reservationService.getDocument(id);
        ctx.json(document);
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getRoomNumber().isEmpty() ||
            request.getGuestName() == null || request.getGuestName().isEmpty() ||
            request.getGuestEmail() == null || request.getGuestEmail().isEmpty() ||
            request.getGuestCpf() == null || request.getGuestCpf().isEmpty() ||
            request.getCheckIn() == null || request.getCheckIn().isEmpty() ||
            request.getCheckOut() == null || request.getCheckOut().isEmpty() ||
            request.getGuests() == null ||
            request.getRatePlan() == null || request.getRatePlan().isEmpty()) {
            throw new InvalidRequestException("Missing required fields");
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }
    }

    private RatePlan parseRatePlan(String ratePlan) {
        try {
            return RatePlan.valueOf(ratePlan);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan");
        }
    }

    private Map<String, Object> toResponse(Reservation reservation) {
        Map<String, Object> response = new LinkedHashMap<>();
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
}
