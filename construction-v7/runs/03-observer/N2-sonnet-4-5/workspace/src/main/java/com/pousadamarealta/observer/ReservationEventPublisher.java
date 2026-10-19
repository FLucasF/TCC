package com.pousadamarealta.observer;

import java.util.ArrayList;
import java.util.List;

public class ReservationEventPublisher {
    private final List<ReservationObserver> observers = new ArrayList<>();

    public void attach(ReservationObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(ReservationObserver observer) {
        observers.remove(observer);
    }

    public void notify(ReservationEvent event) {
        for (ReservationObserver observer : new ArrayList<>(observers)) {
            observer.onReservationEvent(event);
        }
    }
}
