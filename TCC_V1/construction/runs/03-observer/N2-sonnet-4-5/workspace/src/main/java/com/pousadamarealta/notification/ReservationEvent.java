package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;

public sealed interface ReservationEvent {
    Reservation reservation();

    record Confirmed(Reservation reservation) implements ReservationEvent {}
    record Cancelled(Reservation reservation, boolean wasConfirmed) implements ReservationEvent {}
}
