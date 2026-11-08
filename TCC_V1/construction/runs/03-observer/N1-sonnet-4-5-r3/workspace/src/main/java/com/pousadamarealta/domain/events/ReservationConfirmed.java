package com.pousadamarealta.domain.events;

import com.pousadamarealta.domain.Reservation;

public record ReservationConfirmed(Reservation reservation) implements DomainEvent {
}
