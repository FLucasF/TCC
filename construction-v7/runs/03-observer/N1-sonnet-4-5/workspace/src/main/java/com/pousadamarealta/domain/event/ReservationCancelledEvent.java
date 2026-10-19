package com.pousadamarealta.domain.event;

import com.pousadamarealta.domain.model.Reservation;

public record ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed) implements Event {
}
