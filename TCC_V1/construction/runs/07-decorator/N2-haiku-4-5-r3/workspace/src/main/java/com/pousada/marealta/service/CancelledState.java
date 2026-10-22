package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;

public class CancelledState implements ReservationState {
    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Cannot confirm a cancelled reservation");
    }

    @Override
    public Reservation checkIn(Reservation reservation) {
        throw new IllegalStateException("Cannot check in a cancelled reservation");
    }

    @Override
    public Reservation checkOut(Reservation reservation) {
        throw new IllegalStateException("Cannot check out a cancelled reservation");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        throw new IllegalStateException("Reservation is already cancelled");
    }
}
