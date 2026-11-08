package com.pousadamarealta.event;

import com.pousadamarealta.domain.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements Event {
}
