package com.pousada.event;

import com.pousada.model.Reservation;

public abstract class ReservationEvent {
  protected final Reservation reservation;

  protected ReservationEvent(Reservation reservation) {
    this.reservation = reservation;
  }

  public Reservation getReservation() {
    return reservation;
  }
}
