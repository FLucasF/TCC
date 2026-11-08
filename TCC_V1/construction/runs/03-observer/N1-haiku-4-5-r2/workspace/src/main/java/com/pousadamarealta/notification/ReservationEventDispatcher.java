package com.pousadamarealta.notification;

import com.pousadamarealta.model.Reservation;
import java.util.ArrayList;
import java.util.List;

public class ReservationEventDispatcher {
    private static final List<ReservationEventListener> listeners = new ArrayList<>();

    public static void addListener(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ReservationEventListener listener) {
        listeners.remove(listener);
    }

    public static void fireOnConfirmed(Reservation reservation) {
        listeners.forEach(l -> l.onConfirmed(reservation));
    }

    public static void fireOnCancelled(Reservation reservation) {
        listeners.forEach(l -> l.onCancelled(reservation));
    }
}
