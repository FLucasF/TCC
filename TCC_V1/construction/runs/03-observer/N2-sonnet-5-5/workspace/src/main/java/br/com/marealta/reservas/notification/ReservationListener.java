package br.com.marealta.reservas.notification;

/** Quem quer saber do que acontece com as reservas. Cada ouvinte reage só ao que lhe interessa. */
public interface ReservationListener {

    default void onConfirmed(ReservationConfirmed event) {
    }

    default void onCancelled(ReservationCancelled event) {
    }
}
