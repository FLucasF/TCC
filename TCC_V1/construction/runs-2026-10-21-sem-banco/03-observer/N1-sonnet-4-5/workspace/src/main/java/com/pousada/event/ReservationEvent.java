package com.pousada.event;

import com.pousada.domain.Reservation;

public interface ReservationEvent {
    Reservation getReservation();
}
