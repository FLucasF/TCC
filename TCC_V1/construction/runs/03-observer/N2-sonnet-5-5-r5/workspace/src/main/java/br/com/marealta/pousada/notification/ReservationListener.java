package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;

/**
 * Quem quer saber o que acontece com as reservas. Cada interessado implementa só os eventos que lhe importam
 * e decide sozinho se age.
 */
public interface ReservationListener {

    default void onConfirmed(Reservation reservation) {
    }

    /** @param previousStatus situação da reserva antes do cancelamento (PENDING ou CONFIRMED) */
    default void onCancelled(Reservation reservation, ReservationStatus previousStatus) {
    }
}
