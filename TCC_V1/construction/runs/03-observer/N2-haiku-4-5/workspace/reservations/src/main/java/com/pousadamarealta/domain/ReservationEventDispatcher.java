package com.pousadamarealta.domain;

import java.util.ArrayList;
import java.util.List;

public class ReservationEventDispatcher {
    private static final ReservationEventDispatcher INSTANCE = new ReservationEventDispatcher();
    private final List<ReservationEventListener> listeners = new ArrayList<>();

    private ReservationEventDispatcher() {
    }

    public static ReservationEventDispatcher getInstance() {
        return INSTANCE;
    }

    public void addListener(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ReservationEventListener listener) {
        listeners.remove(listener);
    }

    public void fireOnConfirmed(Reservation reservation) {
        for (ReservationEventListener listener : listeners) {
            listener.onConfirmed(reservation);
        }
    }

    public void fireOnCancelled(Reservation reservation) {
        for (ReservationEventListener listener : listeners) {
            listener.onCancelled(reservation);
        }
    }
}
