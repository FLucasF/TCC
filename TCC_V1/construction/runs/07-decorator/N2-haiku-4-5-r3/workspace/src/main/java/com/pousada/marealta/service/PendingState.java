package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;

public class PendingState implements ReservationState {
    @Override
    public Reservation confirm(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        return reservation;
    }

    @Override
    public Reservation checkIn(Reservation reservation) {
        throw new IllegalStateException("Cannot check in a pending reservation");
    }

    @Override
    public Reservation checkOut(Reservation reservation) {
        throw new IllegalStateException("Cannot check out a pending reservation");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(java.math.BigDecimal.ZERO);
        return reservation;
    }
}
