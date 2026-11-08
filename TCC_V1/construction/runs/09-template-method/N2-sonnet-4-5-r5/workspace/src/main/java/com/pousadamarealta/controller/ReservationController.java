package com.pousadamarealta.controller;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.DocumentDto;
import com.pousadamarealta.dto.ReservationDto;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateCreateRequest(request);

        LocalDate checkIn = parseDate(request.checkIn(), "checkIn");
        LocalDate checkOut = parseDate(request.checkOut(), "checkOut");

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            checkIn,
            checkOut,
            request.guests(),
            request.ratePlan()
        );

        ctx.status(201).json(ReservationDto.from(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        Document document = reservationService.getDocument(id);
        ctx.json(DocumentDto.from(document));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Document document = reservationService.getDocument(id);
        ctx.json(DocumentDto.from(document));
    }

    private void validateCreateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new InvalidRequestException("roomNumber is required");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new InvalidRequestException("guestName is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new InvalidRequestException("guestEmail is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new InvalidRequestException("guestCpf is required");
        }
        if (request.checkIn() == null || request.checkIn().isBlank()) {
            throw new InvalidRequestException("checkIn is required");
        }
        if (request.checkOut() == null || request.checkOut().isBlank()) {
            throw new InvalidRequestException("checkOut is required");
        }
        if (request.ratePlan() == null) {
            throw new InvalidRequestException("ratePlan is required");
        }
    }

    private LocalDate parseDate(String date, String fieldName) {
        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format for " + fieldName + ": " + date);
        }
    }
}
