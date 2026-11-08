package com.pousadamarealta.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservationEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);
  private static final List<ReservationEventListener> listeners = new ArrayList<>();

  public static void subscribe(ReservationEventListener listener) {
    listeners.add(listener);
  }

  public static void publishConfirmed(ReservationEvent event) {
    for (ReservationEventListener listener : listeners) {
      try {
        listener.onReservationConfirmed(event);
      } catch (Exception e) {
        log.error("Error notifying listener for confirmation", e);
      }
    }
  }

  public static void publishCancelled(ReservationEvent event) {
    for (ReservationEventListener listener : listeners) {
      try {
        listener.onReservationCancelled(event);
      } catch (Exception e) {
        log.error("Error notifying listener for cancellation", e);
      }
    }
  }

  public static void publishCheckedIn(ReservationEvent event) {
    for (ReservationEventListener listener : listeners) {
      try {
        listener.onCheckedIn(event);
      } catch (Exception e) {
        log.error("Error notifying listener for check-in", e);
      }
    }
  }

  public static void publishCheckedOut(ReservationEvent event) {
    for (ReservationEventListener listener : listeners) {
      try {
        listener.onCheckedOut(event);
      } catch (Exception e) {
        log.error("Error notifying listener for check-out", e);
      }
    }
  }
}
