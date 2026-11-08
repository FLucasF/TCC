package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.model.Reservation;

public class ReservationConfirmedEvent extends ReservationEvent {
    public ReservationConfirmedEvent(Reservation reservation) {
        super(reservation);
    }
}
