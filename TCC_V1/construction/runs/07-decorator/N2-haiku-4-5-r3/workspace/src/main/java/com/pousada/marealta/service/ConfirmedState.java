package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;

public class ConfirmedState implements ReservationState {
    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Reservation is already confirmed");
    }

    @Override
    public Reservation checkIn(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    @Override
    public Reservation checkOut(Reservation reservation) {
        throw new IllegalStateException("Cannot check out without checking in first");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(reservation.getAmountPaid());
        return reservation;
    }
}
