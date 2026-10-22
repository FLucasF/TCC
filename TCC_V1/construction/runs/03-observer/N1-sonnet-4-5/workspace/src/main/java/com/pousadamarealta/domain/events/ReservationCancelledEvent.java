package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.model.Reservation;

public record ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed) implements ReservationEvent {
    @Override
    public Reservation getReservation() {
        return reservation;
    }
}
