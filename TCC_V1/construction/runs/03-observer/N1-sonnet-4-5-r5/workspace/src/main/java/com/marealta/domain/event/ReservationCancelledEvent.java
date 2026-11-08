package com.marealta.domain.event;

import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.ReservationStatus;

public record ReservationCancelledEvent(
    Reservation reservation,
    ReservationStatus previousStatus
) implements DomainEvent {
}
