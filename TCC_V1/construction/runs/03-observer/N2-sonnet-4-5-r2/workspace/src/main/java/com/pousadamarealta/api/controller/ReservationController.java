package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.ApiException;
import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentResponse;
import com.pousadamarealta.api.dto.ReservationResponse;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.ReservationObserver;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReservationController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final List<ReservationObserver> observers;

    public ReservationController(
        RoomRepository roomRepository,
        ReservationRepository reservationRepository,
        List<ReservationObserver> observers
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.observers = observers;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        Room room = roomRepository.findByNumber(request.roomNumber())
            .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Room not found"));

        LocalDate checkIn = parseDate(request.checkIn());
        LocalDate checkOut = parseDate(request.checkOut());

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (request.guests() < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (request.guests() > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        }

        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf(request.ratePlan());
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid rate plan");
        }

        List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(
            request.roomNumber(), checkIn, checkOut
        );

        if (!conflicting.isEmpty()) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        }

        Reservation reservation = new Reservation(
            request.roomNumber(),
            room.getType(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            checkIn,
            checkOut,
            request.guests(),
            ratePlan
        );

        for (ReservationObserver observer : observers) {
            reservation.addObserver(observer);
        }

        reservationRepository.save(reservation);

        ctx.status(201).json(ReservationResponse.from(reservation));
    }

    public void getReservation(Context ctx) {
        long id = parseLongPathParam(ctx, "id");

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));

        ctx.json(ReservationResponse.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = parseLongPathParam(ctx, "id");

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));

        try {
            reservation.confirm();
            reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }

        ctx.json(ReservationResponse.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = parseLongPathParam(ctx, "id");

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));

        try {
            reservation.cancel();
            reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }

        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkIn(Context ctx) {
        long id = parseLongPathParam(ctx, "id");

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));

        try {
            reservation.checkIn();
            reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }

        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkOut(Context ctx) {
        long id = parseLongPathParam(ctx, "id");

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));

        try {
            reservation.checkOut();
            reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }

        ReservationDocument document = new ReservationDocument(reservation);
        ctx.json(DocumentResponse.from(document));
    }

    public void getDocument(Context ctx) {
        long id = parseLongPathParam(ctx, "id");

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Document is only available for paid reservations");
        }

        ReservationDocument document = new ReservationDocument(reservation);
        ctx.json(DocumentResponse.from(document));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Room number is required");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Guest name is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Guest email is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Guest CPF is required");
        }
        if (request.checkIn() == null || request.checkIn().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-in date is required");
        }
        if (request.checkOut() == null || request.checkOut().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out date is required");
        }
        if (request.guests() == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Number of guests is required");
        }
        if (request.ratePlan() == null || request.ratePlan().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Rate plan is required");
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format, expected yyyy-MM-dd");
        }
    }

    private long parseLongPathParam(Context ctx, String param) {
        try {
            return Long.parseLong(ctx.pathParam(param));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid " + param);
        }
    }
}
