package com.pousadamarealta.services;

public interface ReservationEventListener {
  void onReservationConfirmed(ReservationEvent event) throws Exception;

  void onReservationCancelled(ReservationEvent event) throws Exception;

  void onCheckedIn(ReservationEvent event) throws Exception;

  void onCheckedOut(ReservationEvent event) throws Exception;
}
