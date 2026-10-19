package com.pousadamarealta.domain.event;

import com.pousadamarealta.domain.model.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements Event {
}
