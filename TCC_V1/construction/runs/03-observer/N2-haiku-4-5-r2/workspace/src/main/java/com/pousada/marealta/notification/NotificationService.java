package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
  private static final List<ReservationObserver> observers = new ArrayList<>();

  static {
    observers.add(new GuestNotificationObserver());
    observers.add(new GovernanceNotificationObserver());
    observers.add(new FinanceNotificationObserver());
  }

  public static void notifyReservationConfirmed(Reservation reservation) {
    for (ReservationObserver observer : observers) {
      observer.onReservationConfirmed(reservation);
    }
  }

  public static void notifyReservationCancelled(Reservation reservation) {
    for (ReservationObserver observer : observers) {
      observer.onReservationCancelled(reservation);
    }
  }

  public static void registerObserver(ReservationObserver observer) {
    observers.add(observer);
  }

  public static void unregisterObserver(ReservationObserver observer) {
    observers.remove(observer);
  }
}
