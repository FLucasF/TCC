package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;

public interface ReservationObserver {
  void onReservationConfirmed(Reservation reservation);
  void onReservationCancelled(Reservation reservation);
}
