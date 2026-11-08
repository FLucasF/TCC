package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentResponse;
import com.pousadamarealta.api.dto.ReservationResponse;
import com.pousadamarealta.domain.model.RatePlan;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.sql.SQLException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) throws SQLException {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        RatePlan ratePlan = RatePlan.valueOf(request.getRatePlan());

        Reservation reservation = reservationService.createReservation(
            request.getRoomNumber(),
            request.getGuestName(),
            request.getGuestEmail(),
            request.getGuestCpf(),
            request.getCheckIn(),
            request.getCheckOut(),
            request.getGuests(),
            ratePlan
        );

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.status(201).json(new ReservationResponse(reservation, room));
    }

    public void getReservation(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(new ReservationResponse(reservation, room));
    }

    public void confirmReservation(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(new ReservationResponse(reservation, room));
    }

    public void cancelReservation(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(new ReservationResponse(reservation, room));
    }

    public void checkIn(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(new ReservationResponse(reservation, room));
    }

    public void checkOut(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(createDocumentResponse(reservation, room));
    }

    public void getDocument(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        ctx.json(createDocumentResponse(reservation, room));
    }

    private DocumentResponse createDocumentResponse(Reservation reservation, Room room) {
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        return new DocumentResponse(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getNights(),
            subtotal,
            discount,
            total
        );
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getGuestCpf() == null ||
            request.getCheckIn() == null || request.getCheckOut() == null ||
            request.getGuests() == null || request.getRatePlan() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
    }
}
