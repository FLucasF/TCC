package br.com.pousadamarealta.domain.events;

import br.com.pousadamarealta.domain.Reservation;

public record ReservationConfirmed(Reservation reservation) {
}
