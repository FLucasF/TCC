package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.model.Reservation;

public interface ReservationEvent {
    Reservation getReservation();
}
