package com.pousadamarealta.notification.event;

import com.pousadamarealta.domain.Reservation;

public record ReservationConfirmedEvent(Reservation reservation) implements ReservationEvent {}
