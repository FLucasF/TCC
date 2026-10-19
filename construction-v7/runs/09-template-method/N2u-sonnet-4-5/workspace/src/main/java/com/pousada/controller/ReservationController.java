package com.pousada.controller;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.dto.CreateReservationRequest;
import com.pousada.repository.RoomRepository;
import com.pousada.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        LocalDate checkIn = LocalDate.parse(request.getCheckIn(), dateFormatter);
        LocalDate checkOut = LocalDate.parse(request.getCheckOut(), dateFormatter);

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        RatePlan ratePlan = RatePlan.valueOf(request.getRatePlan());

        Reservation reservation = reservationService.createReservation(
                request.getRoomNumber(),
                request.getGuestName(),
                request.getGuestEmail(),
                request.getGuestCpf(),
                checkIn,
                checkOut,
                request.getGuests(),
                ratePlan
        );

        ctx.status(201).json(reservationToMap(reservation));
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(reservationToMap(reservation));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(subtotal, discount);

        Map<String, Object> document = new HashMap<>();
        document.put("title", "Comprovante de reserva");
        document.put("reservationId", reservation.getId());
        document.put("guestName", reservation.getGuestName());
        document.put("guestCpf", reservation.getGuestCpf());
        document.put("roomNumber", reservation.getRoomNumber());
        document.put("roomType", room.getType().toString());
        document.put("checkIn", reservation.getCheckIn().format(dateFormatter));
        document.put("checkOut", reservation.getCheckOut().format(dateFormatter));
        document.put("nights", reservation.getNights());
        document.put("subtotal", subtotal);
        document.put("discount", discount);
        document.put("total", total);

        ctx.json(document);
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(subtotal, discount);

        Map<String, Object> document = new HashMap<>();
        document.put("title", "Comprovante de reserva");
        document.put("reservationId", reservation.getId());
        document.put("guestName", reservation.getGuestName());
        document.put("guestCpf", reservation.getGuestCpf());
        document.put("roomNumber", reservation.getRoomNumber());
        document.put("roomType", room.getType().toString());
        document.put("checkIn", reservation.getCheckIn().format(dateFormatter));
        document.put("checkOut", reservation.getCheckOut().format(dateFormatter));
        document.put("nights", reservation.getNights());
        document.put("subtotal", subtotal);
        document.put("discount", discount);
        document.put("total", total);

        ctx.json(document);
    }

    private Map<String, Object> reservationToMap(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(subtotal, discount);

        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().format(dateFormatter));
        map.put("checkOut", reservation.getCheckOut().format(dateFormatter));
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().toString());
        map.put("nights", reservation.getNights());
        map.put("subtotal", subtotal);
        map.put("discount", discount);
        map.put("total", total);
        map.put("status", reservation.getStatus().toString());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == 0 ||
            request.getGuestName() == null || request.getGuestName().isBlank() ||
            request.getGuestEmail() == null || request.getGuestEmail().isBlank() ||
            request.getGuestCpf() == null || request.getGuestCpf().isBlank() ||
            request.getCheckIn() == null || request.getCheckIn().isBlank() ||
            request.getCheckOut() == null || request.getCheckOut().isBlank() ||
            request.getRatePlan() == null || request.getRatePlan().isBlank()) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        try {
            LocalDate.parse(request.getCheckIn(), dateFormatter);
            LocalDate.parse(request.getCheckOut(), dateFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        try {
            RatePlan.valueOf(request.getRatePlan());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }
}
