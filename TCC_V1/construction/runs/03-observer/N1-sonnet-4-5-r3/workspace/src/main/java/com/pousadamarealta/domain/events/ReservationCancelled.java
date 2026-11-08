package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.Reservation;

public record ReservationCancelled(Reservation reservation, boolean wasConfirmed) implements DomainEvent {
}
