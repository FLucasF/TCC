package com.pousadamarealta.notification;

import com.pousadamarealta.notification.event.ReservationEvent;
import java.util.ArrayList;
import java.util.List;

public class EventDispatcher {
    private final List<ReservationEventListener> listeners = new ArrayList<>();

    public void subscribe(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(ReservationEventListener listener) {
        listeners.remove(listener);
    }

    public void dispatch(ReservationEvent event) {
        for (var listener : listeners) {
            listener.onReservationEvent(event);
        }
    }
}
