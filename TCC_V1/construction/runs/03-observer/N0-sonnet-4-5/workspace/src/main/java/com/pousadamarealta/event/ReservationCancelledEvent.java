package com.pousadamarealta.event;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

public class ReservationCancelledEvent extends ReservationEvent {
    private final ReservationStatus previousStatus;

    public ReservationCancelledEvent(Reservation reservation, ReservationStatus previousStatus) {
        super(reservation);
        this.previousStatus = previousStatus;
    }

    public ReservationStatus getPreviousStatus() {
        return previousStatus;
    }

    public boolean wasConfirmed() {
        return previousStatus == ReservationStatus.CONFIRMED;
    }
}
