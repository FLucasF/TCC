package com.pousadamarealta.notification.event;

import com.pousadamarealta.domain.Reservation;

public sealed interface ReservationEvent permits ReservationConfirmedEvent, ReservationCancelledEvent {
    Reservation reservation();
}
