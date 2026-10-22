package br.com.pousadamarealta.service.observer;

import br.com.pousadamarealta.domain.Reservation;

public interface ReservationObserver {
    void onReservationConfirmed(Reservation reservation);
    void onReservationCancelled(Reservation reservation, boolean wasConfirmed);
}
