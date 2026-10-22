package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.model.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements ReservationEvent {
    @Override
    public Reservation getReservation() {
        return reservation;
    }
}
