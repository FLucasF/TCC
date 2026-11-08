package com.pousadamarealta.domain.event;

import java.util.ArrayList;
import java.util.List;

public class EventPublisher {
    private final List<ReservationConfirmedListener> confirmedListeners = new ArrayList<>();
    private final List<ReservationCancelledListener> cancelledListeners = new ArrayList<>();

    public void registerConfirmedListener(ReservationConfirmedListener listener) {
        confirmedListeners.add(listener);
    }

    public void registerCancelledListener(ReservationCancelledListener listener) {
        cancelledListeners.add(listener);
    }

    public void publishConfirmed(ReservationConfirmedEvent event) {
        for (ReservationConfirmedListener listener : confirmedListeners) {
            listener.onReservationConfirmed(event);
        }
    }

    public void publishCancelled(ReservationCancelledEvent event) {
        for (ReservationCancelledListener listener : cancelledListeners) {
            listener.onReservationCancelled(event);
        }
    }

    public interface ReservationConfirmedListener {
        void onReservationConfirmed(ReservationConfirmedEvent event);
    }

    public interface ReservationCancelledListener {
        void onReservationCancelled(ReservationCancelledEvent event);
    }
}
