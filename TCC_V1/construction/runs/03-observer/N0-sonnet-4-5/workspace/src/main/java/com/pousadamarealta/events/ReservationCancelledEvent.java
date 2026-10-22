package com.pousadamarealta.events;

import com.pousadamarealta.domain.Reservation;

public record ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed) implements Event {
}
