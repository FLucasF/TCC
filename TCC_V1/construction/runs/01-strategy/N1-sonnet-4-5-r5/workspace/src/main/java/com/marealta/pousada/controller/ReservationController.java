package com.marealta.pousada.controller;

import com.marealta.pousada.domain.model.Reservation;
import com.marealta.pousada.dto.CreateReservationRequest;
import com.marealta.pousada.dto.DocumentDto;
import com.marealta.pousada.dto.ReservationDto;
import com.marealta.pousada.exception.InvalidRequestException;
import com.marealta.pousada.exception.OperationNotAllowedException;
import com.marealta.pousada.service.ReservationService;
import io.javalin.http.Context;

public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            request.ratePlan(),
            request.companyCode()
        );

        ctx.status(201).json(ReservationDto.from(reservation));
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkInReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkInReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkOutReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOutReservation(id);
        ctx.json(DocumentDto.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        ctx.json(DocumentDto.from(reservation));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new InvalidRequestException("Guest name is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new InvalidRequestException("Guest email is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new InvalidRequestException("Guest CPF is required");
        }
        if (request.checkIn() == null) {
            throw new InvalidRequestException("Check-in date is required");
        }
        if (request.checkOut() == null) {
            throw new InvalidRequestException("Check-out date is required");
        }
        if (request.ratePlan() == null || request.ratePlan().isBlank()) {
            throw new InvalidRequestException("Rate plan is required");
        }
    }
}
