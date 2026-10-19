package com.pousadamarealta.observer;

import com.pousadamarealta.model.Reservation;

public class ReservationEvent {
    private final ReservationEventType type;
    private final Reservation reservation;
    private final Reservation previousState;

    public ReservationEvent(ReservationEventType type, Reservation reservation, Reservation previousState) {
        this.type = type;
        this.reservation = reservation;
        this.previousState = previousState;
    }

    public ReservationEventType getType() {
        return type;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Reservation getPreviousState() {
        return previousState;
    }
}
