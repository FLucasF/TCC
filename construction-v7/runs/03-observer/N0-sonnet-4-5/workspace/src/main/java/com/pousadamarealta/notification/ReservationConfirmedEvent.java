package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements ReservationEvent {
}
