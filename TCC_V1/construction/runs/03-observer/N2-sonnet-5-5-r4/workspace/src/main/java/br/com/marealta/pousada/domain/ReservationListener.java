package br.com.marealta.pousada.domain;

/**
 * Quem quer saber o que acontece com as reservas. Cada ouvinte implementa só os eventos que lhe interessam
 * e decide sozinho se age.
 */
public interface ReservationListener {

    default void onConfirmed(Reservation reservation) {
    }

    /** {@code previous} é a situação que a reserva tinha antes de ser cancelada. */
    default void onCancelled(Reservation reservation, ReservationStatus previous) {
    }
}
