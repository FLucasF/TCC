package br.com.pousadamarealta.domain.events;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.ReservationStatus;

public record ReservationCancelled(Reservation reservation, ReservationStatus previousStatus) {
}
