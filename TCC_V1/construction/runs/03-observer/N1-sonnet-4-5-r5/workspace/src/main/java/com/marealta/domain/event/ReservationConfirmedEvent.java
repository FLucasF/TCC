package com.marealta.domain.event;

import com.marealta.domain.model.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements DomainEvent {
}
