package br.com.pousadamarealta.events;

import br.com.pousadamarealta.domain.Reservation;

public interface ReservationEvent {
    Reservation getReservation();
}
