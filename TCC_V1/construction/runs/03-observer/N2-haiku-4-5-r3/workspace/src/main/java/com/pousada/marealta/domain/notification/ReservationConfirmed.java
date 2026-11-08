package com.pousada.marealta.domain.notification;

import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.reservation.ReservationStatus;

public class ReservationConfirmed extends DomainEvent {
    private final Reservation reservation;
    private final ReservationStatus previousStatus;

    public ReservationConfirmed(Reservation reservation, ReservationStatus previousStatus) {
        this.reservation = reservation;
        this.previousStatus = previousStatus;
    }

    public Reservation reservation() {
        return reservation;
    }

    public ReservationStatus previousStatus() {
        return previousStatus;
    }
}
