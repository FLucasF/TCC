package com.pousadamarealta.events;

import com.pousadamarealta.domain.Reservation;
import java.util.ArrayList;
import java.util.List;

public class ReservationEventDispatcher {
    private final List<ReservationEventListener> listeners = new ArrayList<>();

    public void addListener(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void dispatchReservationConfirmed(Reservation reservation) {
        for (ReservationEventListener listener : listeners) {
            listener.onReservationConfirmed(reservation);
        }
    }

    public void dispatchReservationCancelled(Reservation reservation) {
        for (ReservationEventListener listener : listeners) {
            listener.onReservationCancelled(reservation);
        }
    }
}
