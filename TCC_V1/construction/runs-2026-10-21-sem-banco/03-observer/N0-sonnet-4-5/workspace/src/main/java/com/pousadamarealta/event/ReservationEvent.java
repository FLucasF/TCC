package com.pousadamarealta.event;

import com.pousadamarealta.domain.Reservation;

public abstract class ReservationEvent {
    private final Reservation reservation;

    protected ReservationEvent(Reservation reservation) {
        this.reservation = reservation;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
