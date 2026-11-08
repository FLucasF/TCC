package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.model.Reservation;

public class ReservationCancelledEvent extends ReservationEvent {
    private final boolean wasConfirmed;

    public ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed) {
        super(reservation);
        this.wasConfirmed = wasConfirmed;
    }

    public boolean wasConfirmed() {
        return wasConfirmed;
    }
}
