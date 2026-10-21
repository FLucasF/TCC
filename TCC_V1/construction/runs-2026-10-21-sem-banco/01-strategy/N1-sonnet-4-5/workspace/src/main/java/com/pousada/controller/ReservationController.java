package com.pousada.controller;

import com.pousada.dto.CreateReservationRequest;
import com.pousada.dto.DocumentResponse;
import com.pousada.dto.ReservationResponse;
import com.pousada.exception.InvalidRequestException;
import com.pousada.model.Reservation;
import com.pousada.model.RatePlanType;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
    }

    private void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequired(request.roomNumber, "roomNumber");
        validateRequired(request.guestName, "guestName");
        validateRequired(request.guestEmail, "guestEmail");
        validateRequired(request.guestCpf, "guestCpf");
        validateRequired(request.checkIn, "checkIn");
        validateRequired(request.checkOut, "checkOut");
        validateRequired(request.guests, "guests");
        validateRequired(request.ratePlan, "ratePlan");

        LocalDate checkIn = parseDate(request.checkIn);
        LocalDate checkOut = parseDate(request.checkOut);
        RatePlanType ratePlanType = parseRatePlan(request.ratePlan);

        Reservation reservation = reservationService.createReservation(
            request.roomNumber,
            request.guestName,
            request.guestEmail,
            request.guestCpf,
            checkIn,
            checkOut,
            request.guests,
            ratePlanType,
            request.companyCode
        );

        ctx.status(201).json(new ReservationResponse(reservation));
    }

    private void getReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void confirmReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void cancelReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void checkIn(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private void checkOut(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);

        DocumentResponse document = new DocumentResponse(
            reservation,
            roomService.getRoom(reservation.getRoomNumber()).getType()
        );

        ctx.json(document);
    }

    private void getDocument(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);

        DocumentResponse document = new DocumentResponse(
            reservation,
            roomService.getRoom(reservation.getRoomNumber()).getType()
        );

        ctx.json(document);
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new InvalidRequestException("Campo obrigatório ausente: " + fieldName);
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de data inválido");
        }
    }

    private RatePlanType parseRatePlan(String ratePlan) {
        try {
            return RatePlanType.valueOf(ratePlan);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Plano de tarifa inválido");
        }
    }
}
