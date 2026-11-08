package com.pousadamarealta.events;

import com.pousadamarealta.domain.Reservation;

public interface ReservationEventListener {
    void onReservationConfirmed(Reservation reservation);
    void onReservationCancelled(Reservation reservation);
}
