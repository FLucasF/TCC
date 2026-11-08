package com.pousada.marealta.notification;

import com.pousada.marealta.model.Reservation;
import java.math.BigDecimal;

public class ReservationEvent {
    public enum Type {
        CONFIRMED,
        CANCELLED
    }

    private final Type type;
    private final Reservation reservation;

    public ReservationEvent(Type type, Reservation reservation) {
        this.type = type;
        this.reservation = reservation;
    }

    public Type getType() {
        return type;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
