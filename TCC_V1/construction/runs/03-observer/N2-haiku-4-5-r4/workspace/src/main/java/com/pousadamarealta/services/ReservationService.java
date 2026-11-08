package com.pousadamarealta.services;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.events.ReservationEventDispatcher;
import com.pousadamarealta.utils.PricingCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final Database database;
    private final ReservationEventDispatcher eventDispatcher;

    public ReservationService(Database database, ReservationEventDispatcher eventDispatcher) {
        this.database = database;
        this.eventDispatcher = eventDispatcher;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Optional<Room> room = database.getRoomByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.get().getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!database.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.get().getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total
        );

        long reservationId = database.insertReservation(reservation);
        reservation = new Reservation(
            reservationId,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total
        );

        return reservation;
    }

    public Reservation confirmReservation(long reservationId) {
        Optional<Reservation> reservation = database.getReservationById(reservationId);
        if (reservation.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation res = reservation.get();
        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        res.setStatus(ReservationStatus.CONFIRMED);
        res.setAmountPaid(res.getTotal());
        database.updateReservation(res);

        eventDispatcher.dispatchReservationConfirmed(res);

        return res;
    }

    public Reservation cancelReservation(long reservationId) {
        Optional<Reservation> reservation = database.getReservationById(reservationId);
        if (reservation.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation res = reservation.get();
        if (res.getStatus() == ReservationStatus.CHECKED_IN ||
            res.getStatus() == ReservationStatus.CHECKED_OUT ||
            res.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        boolean wasConfirmed = res.getStatus() == ReservationStatus.CONFIRMED;

        res.setStatus(ReservationStatus.CANCELLED);

        if (wasConfirmed) {
            if (res.getRatePlan() == RatePlan.STANDARD) {
                res.setRefund(res.getTotal());
            } else {
                res.setRefund(BigDecimal.ZERO);
            }
        } else {
            res.setRefund(BigDecimal.ZERO);
        }

        database.updateReservation(res);

        if (wasConfirmed) {
            eventDispatcher.dispatchReservationCancelled(res);
        }

        return res;
    }

    public Reservation checkIn(long reservationId) {
        Optional<Reservation> reservation = database.getReservationById(reservationId);
        if (reservation.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation res = reservation.get();
        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        res.setStatus(ReservationStatus.CHECKED_IN);
        database.updateReservation(res);

        return res;
    }

    public Reservation checkOut(long reservationId) {
        Optional<Reservation> reservation = database.getReservationById(reservationId);
        if (reservation.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation res = reservation.get();
        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        res.setStatus(ReservationStatus.CHECKED_OUT);
        database.updateReservation(res);

        return res;
    }
}
