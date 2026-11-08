package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;

public interface NotificationListener {
    void onReservationConfirmed(Reservation reservation);
    void onReservationCancelled(Reservation reservation);
}
