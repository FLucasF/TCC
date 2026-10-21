package com.pousadamarealta.notification;

import com.pousadamarealta.notification.event.ReservationEvent;

public interface ReservationEventListener {
    void onReservationEvent(ReservationEvent event);
}
