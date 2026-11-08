package com.pousadamarealta.notification;

import com.pousadamarealta.model.Reservation;

public interface ReservationEventListener {
    void onConfirmed(Reservation reservation);
    void onCancelled(Reservation reservation);
}
