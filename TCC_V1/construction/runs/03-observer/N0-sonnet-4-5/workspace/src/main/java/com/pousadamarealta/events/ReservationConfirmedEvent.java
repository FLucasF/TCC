package com.pousadamarealta.events;

import com.pousadamarealta.domain.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements Event {
}
