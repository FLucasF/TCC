package com.pousadamarealta.event;

import com.pousadamarealta.domain.Reservation;

public class ReservationConfirmedEvent extends ReservationEvent {
    public ReservationConfirmedEvent(Reservation reservation) {
        super(reservation);
    }
}
