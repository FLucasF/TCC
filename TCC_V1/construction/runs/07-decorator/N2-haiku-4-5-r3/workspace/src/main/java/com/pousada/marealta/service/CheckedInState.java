package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;

public class CheckedInState implements ReservationState {
    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Cannot confirm a checked-in reservation");
    }

    @Override
    public Reservation checkIn(Reservation reservation) {
        throw new IllegalStateException("Reservation is already checked in");
    }

    @Override
    public Reservation checkOut(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        throw new IllegalStateException("Cannot cancel a checked-in reservation");
    }
}
