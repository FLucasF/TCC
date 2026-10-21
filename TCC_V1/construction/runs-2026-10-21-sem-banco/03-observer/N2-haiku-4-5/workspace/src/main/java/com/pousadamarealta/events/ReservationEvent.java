package com.pousadamarealta.events;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

public class ReservationEvent {
    public enum Type {
        CONFIRMED,
        CANCELLED
    }

    private final Type type;
    private final Reservation reservation;
    private final ReservationStatus previousStatus;

    public ReservationEvent(Type type, Reservation reservation, ReservationStatus previousStatus) {
        this.type = type;
        this.reservation = reservation;
        this.previousStatus = previousStatus;
    }

    public Type getType() {
        return type;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public ReservationStatus getPreviousStatus() {
        return previousStatus;
    }
}
