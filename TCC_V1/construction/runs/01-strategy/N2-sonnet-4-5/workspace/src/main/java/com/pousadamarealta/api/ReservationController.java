package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.domain.RatePlanType;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        RatePlanType ratePlanType = parseRatePlan(request.ratePlan());

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            ratePlanType,
            request.companyCode()
        );

        ctx.status(201).json(ReservationDTO.from(reservation));
    }

    public void getReservation(Context ctx) {
        long id = parseId(ctx);
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = parseId(ctx);
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void checkInReservation(Context ctx) {
        long id = parseId(ctx);
        Reservation reservation = reservationService.checkInReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void checkOutReservation(Context ctx) {
        long id = parseId(ctx);
        Reservation reservation = reservationService.checkOutReservation(id);
        ctx.json(DocumentDTO.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = parseId(ctx);
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void getDocument(Context ctx) {
        long id = parseId(ctx);
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new InvalidRequestException("Document only available for paid reservations");
        }

        ctx.json(DocumentDTO.from(reservation));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isEmpty()) {
            throw new InvalidRequestException("Room number is required");
        }
        if (request.guestName() == null || request.guestName().isEmpty()) {
            throw new InvalidRequestException("Guest name is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isEmpty()) {
            throw new InvalidRequestException("Guest email is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isEmpty()) {
            throw new InvalidRequestException("Guest CPF is required");
        }
        if (request.checkIn() == null) {
            throw new InvalidRequestException("Check-in date is required");
        }
        if (request.checkOut() == null) {
            throw new InvalidRequestException("Check-out date is required");
        }
        if (request.checkOut().isBefore(request.checkIn()) ||
            request.checkOut().equals(request.checkIn())) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }
        if (request.guests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }
        if (request.ratePlan() == null || request.ratePlan().isEmpty()) {
            throw new InvalidRequestException("Rate plan is required");
        }
    }

    private RatePlanType parseRatePlan(String ratePlan) {
        try {
            return RatePlanType.valueOf(ratePlan);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan: " + ratePlan);
        }
    }

    private long parseId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid reservation ID");
        }
    }
}
