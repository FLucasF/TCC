package com.pousada.api;

import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.DocumentDto;
import com.pousada.api.dto.ReservationDto;
import com.pousada.domain.GuestIdentification;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.service.InvalidRequestException;
import com.pousada.service.ReservationService;
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

        GuestIdentification identification = parseIdentification(request);
        LocalDate checkIn = parseDate(request.checkIn());
        LocalDate checkOut = parseDate(request.checkOut());
        RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            identification,
            checkIn,
            checkOut,
            request.guests(),
            ratePlan
        );

        ctx.status(201).json(ReservationDto.from(reservation));
    }

    public void getReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkIn(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkOut(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(DocumentDto.from(reservation));
    }

    public void getDocument(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new InvalidRequestException("Document is only available for paid reservations");
        }

        ctx.json(DocumentDto.from(reservation));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.checkIn() == null ||
            request.checkOut() == null || request.ratePlan() == null) {
            throw new InvalidRequestException("Missing required fields");
        }

        if (request.guests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }
    }

    private GuestIdentification parseIdentification(CreateReservationRequest request) {
        boolean hasCpf = request.guestCpf() != null;
        boolean hasCnpj = request.companyCnpj() != null && request.companyName() != null;
        boolean hasPassport = request.guestPassport() != null && request.guestCountry() != null;

        int count = (hasCpf ? 1 : 0) + (hasCnpj ? 1 : 0) + (hasPassport ? 1 : 0);
        if (count != 1) {
            throw new InvalidRequestException("Exactly one type of identification is required");
        }

        if (hasCpf) {
            return new GuestIdentification.Cpf(request.guestCpf());
        } else if (hasCnpj) {
            return new GuestIdentification.Cnpj(request.companyCnpj(), request.companyName());
        } else {
            return new GuestIdentification.Passport(request.guestPassport(), request.guestCountry());
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + date);
        }
    }
}
