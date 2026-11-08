package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;

public interface ReservationEventListener {
    void onReservationConfirmed(Reservation reservation, RoomType roomType);
    void onReservationCancelled(Reservation reservation, RoomType roomType, boolean wasPreviouslyConfirmed);
}
