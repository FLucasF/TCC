package com.pousadamarealta.controller;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.DocumentDTO;
import com.pousadamarealta.dto.ReservationDTO;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
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

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        LocalDate checkIn = LocalDate.parse(request.checkIn());
        LocalDate checkOut = LocalDate.parse(request.checkOut());

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

        ctx.status(201).json(ReservationDTO.from(reservation));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null ||
            request.guests() == null || request.ratePlan() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id)
            .orElseThrow(() -> new com.pousadamarealta.service.ReservationNotFoundException(id));
        ctx.json(ReservationDTO.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void checkInReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkInReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void checkOutReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOutReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(DocumentDTO.from(reservation, room));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id)
            .orElseThrow(() -> new com.pousadamarealta.service.ReservationNotFoundException(id));

        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new com.pousadamarealta.service.OperationNotAllowedException(
                "Document only available for paid reservations");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(DocumentDTO.from(reservation, room));
    }
}
