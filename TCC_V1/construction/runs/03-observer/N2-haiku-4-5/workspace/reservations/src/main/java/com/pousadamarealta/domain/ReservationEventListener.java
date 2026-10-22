package com.pousadamarealta.domain;

public interface ReservationEventListener {
    void onConfirmed(Reservation reservation);
    void onCancelled(Reservation reservation);
}
