package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;

public sealed interface ReservationEvent permits ReservationConfirmedEvent, ReservationCancelledEvent {
    Reservation reservation();
}
