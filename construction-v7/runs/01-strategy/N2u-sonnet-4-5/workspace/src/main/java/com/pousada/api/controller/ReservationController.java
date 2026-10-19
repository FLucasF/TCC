package com.pousada.api.controller;

import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.ReceiptResponse;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.domain.Reservation;
import com.pousada.domain.RoomType;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.http.Context;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

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

        ctx.status(201).json(ReservationResponse.from(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);

        RoomType roomType = roomService.findByNumber(reservation.getRoomNumber())
            .map(room -> room.getType())
            .orElseThrow();

        ctx.json(ReceiptResponse.from(reservation, roomType));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new IllegalStateException("Document only available for paid reservations");
        }

        RoomType roomType = roomService.findByNumber(reservation.getRoomNumber())
            .map(room -> room.getType())
            .orElseThrow();

        ctx.json(ReceiptResponse.from(reservation, roomType));
    }
}
