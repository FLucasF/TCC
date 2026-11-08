package br.com.marealta.notification;

import br.com.marealta.domain.Reservation;

/** Quem quer saber o que acontece com as reservas. Cada ouvinte decide se o fato lhe interessa. */
public interface ReservationListener {

    default void onConfirmed(Reservation reservation) {
    }

    default void onCancelled(Reservation reservation) {
    }
}
