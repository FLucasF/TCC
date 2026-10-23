package com.pousadamarealta.controller;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.dto.CreateReservationDTO;
import com.pousadamarealta.dto.DocumentDTO;
import com.pousadamarealta.dto.ReservationDTO;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import com.pousadamarealta.util.RequestValidator;
import io.javalin.http.Context;

import java.time.LocalDate;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        CreateReservationDTO dto = ctx.bodyAsClass(CreateReservationDTO.class);
        RequestValidator.validateCreateReservation(dto);

        LocalDate checkIn = LocalDate.parse(dto.checkIn());
        LocalDate checkOut = LocalDate.parse(dto.checkOut());
        RatePlan ratePlan = RatePlan.valueOf(dto.ratePlan());

        Reservation reservation = reservationService.createReservation(
            dto.roomNumber(),
            dto.guestName(),
            dto.guestEmail(),
            dto.guestCpf(),
            checkIn,
            checkOut,
            dto.guests(),
            ratePlan
        );

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.status(201).json(ReservationDTO.from(reservation, room));
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(ReservationDTO.from(reservation, room));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(ReservationDTO.from(reservation, room));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(ReservationDTO.from(reservation, room));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(DocumentDTO.from(reservation, room));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(ReservationDTO.from(reservation, room));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        if (reservation.getAmountPaid().signum() == 0) {
            throw new IllegalStateException("Document only available for paid reservations");
        }

        ctx.json(DocumentDTO.from(reservation, room));
    }
}
