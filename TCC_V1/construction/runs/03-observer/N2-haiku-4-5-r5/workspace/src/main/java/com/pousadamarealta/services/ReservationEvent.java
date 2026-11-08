package com.pousadamarealta.services;

import com.pousadamarealta.domain.Reservation;

public record ReservationEvent(
    EventType type,
    Reservation reservation,
    Reservation previousReservation // for cancelled events, this is the state before cancellation
    ) {
  public enum EventType {
    CONFIRMED,
    CANCELLED,
    CHECKED_IN,
    CHECKED_OUT
  }

  public ReservationEvent(EventType type, Reservation reservation) {
    this(type, reservation, null);
  }
}
