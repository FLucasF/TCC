package com.pousadamarealta.domain.event;

import com.pousadamarealta.domain.model.Reservation;

public class ReservationCancelledEvent {
    private final Reservation reservation;
    private final boolean wasConfirmed;

    public ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed) {
        this.reservation = reservation;
        this.wasConfirmed = wasConfirmed;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public boolean wasConfirmed() {
        return wasConfirmed;
    }
}
