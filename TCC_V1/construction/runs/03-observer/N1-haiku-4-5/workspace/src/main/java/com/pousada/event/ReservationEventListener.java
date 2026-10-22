package com.pousada.event;

public interface ReservationEventListener {
  void onReservationConfirmed(ReservationConfirmedEvent event);

  void onReservationCancelled(ReservationCancelledEvent event);
}
