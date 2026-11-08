package com.pousadamarealta.event;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

public record ReservationCancelledEvent(Reservation reservation, ReservationStatus previousStatus) implements Event {
}
