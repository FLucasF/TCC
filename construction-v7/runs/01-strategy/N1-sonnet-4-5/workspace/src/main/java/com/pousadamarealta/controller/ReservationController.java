package com.pousadamarealta.controller;

import com.pousadamarealta.dto.*;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
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

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationDto.from(reservation));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(DocumentDto.from(reservation, room));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();

        if (reservation.getStatus().name().equals("PENDING") ||
            reservation.getStatus().name().equals("CANCELLED")) {
            throw new ReservationService.OperationNotAllowedException();
        }

        ctx.json(DocumentDto.from(reservation, room));
    }
}
