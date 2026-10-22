package com.pousada.event;

import java.util.ArrayList;
import java.util.List;

public class EventDispatcher {
  private static final List<ReservationEventListener> listeners = new ArrayList<>();

  public static void subscribe(ReservationEventListener listener) {
    listeners.add(listener);
  }

  public static void dispatchConfirmed(ReservationConfirmedEvent event) {
    for (ReservationEventListener listener : listeners) {
      listener.onReservationConfirmed(event);
    }
  }

  public static void dispatchCancelled(ReservationCancelledEvent event) {
    for (ReservationEventListener listener : listeners) {
      listener.onReservationCancelled(event);
    }
  }
}
