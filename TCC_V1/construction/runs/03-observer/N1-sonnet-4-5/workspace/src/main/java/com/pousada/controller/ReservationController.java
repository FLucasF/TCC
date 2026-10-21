package com.pousada.controller;

import com.pousada.domain.Reservation;
import com.pousada.dto.CreateReservationRequest;
import com.pousada.dto.DocumentResponse;
import com.pousada.dto.ReservationResponse;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import com.pousada.util.DateFormatter;
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
            request.getRoomNumber(),
            request.getGuestName(),
            request.getGuestEmail(),
            request.getGuestCpf(),
            DateFormatter.parseISO(request.getCheckIn()),
            DateFormatter.parseISO(request.getCheckOut()),
            request.getGuests(),
            request.getRatePlan()
        );

        ctx.status(201).json(ReservationResponse.from(reservation, roomService));
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation, roomService));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation, roomService));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation, roomService));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation, roomService));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(DocumentResponse.from(reservation, roomService));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new IllegalStateException("Document only available for paid reservations");
        }

        ctx.json(DocumentResponse.from(reservation, roomService));
    }
}
