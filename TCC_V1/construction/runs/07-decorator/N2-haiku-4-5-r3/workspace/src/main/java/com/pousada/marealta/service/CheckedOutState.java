package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;

public class CheckedOutState implements ReservationState {
    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Cannot confirm a checked-out reservation");
    }

    @Override
    public Reservation checkIn(Reservation reservation) {
        throw new IllegalStateException("Cannot check in a checked-out reservation");
    }

    @Override
    public Reservation checkOut(Reservation reservation) {
        throw new IllegalStateException("Reservation is already checked out");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        throw new IllegalStateException("Cannot cancel a checked-out reservation");
    }
}
