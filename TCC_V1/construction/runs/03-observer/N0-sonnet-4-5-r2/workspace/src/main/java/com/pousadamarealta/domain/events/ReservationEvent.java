package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.model.Reservation;

public abstract class ReservationEvent {
    private final Reservation reservation;

    protected ReservationEvent(Reservation reservation) {
        this.reservation = reservation;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
