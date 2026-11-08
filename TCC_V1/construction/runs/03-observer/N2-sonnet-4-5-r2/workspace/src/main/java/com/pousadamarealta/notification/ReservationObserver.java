package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;

public interface ReservationObserver {
    void onReservationConfirmed(Reservation reservation);
    void onReservationCancelled(Reservation reservation, boolean wasConfirmed);
}
