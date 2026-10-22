package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.ReservationStatus;
import br.com.pousadamarealta.service.observer.ReservationObserver;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final List<ReservationObserver> observers = new ArrayList<>();

    public void addObserver(ReservationObserver observer) {
        observers.add(observer);
    }

    public void notifyConfirmation(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationConfirmed(reservation);
        }
    }

    public void notifyCancellation(Reservation reservation, boolean wasConfirmed) {
        for (ReservationObserver observer : observers) {
            observer.onReservationCancelled(reservation, wasConfirmed);
        }
    }
}
