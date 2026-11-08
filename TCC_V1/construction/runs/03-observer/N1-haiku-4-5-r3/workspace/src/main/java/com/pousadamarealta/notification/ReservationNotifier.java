package com.pousadamarealta.notification;

import com.pousadamarealta.model.Reservation;

public interface ReservationNotifier {
    void onReservationConfirmed(Reservation reservation) throws Exception;
    void onReservationCancelled(Reservation reservation) throws Exception;
}
