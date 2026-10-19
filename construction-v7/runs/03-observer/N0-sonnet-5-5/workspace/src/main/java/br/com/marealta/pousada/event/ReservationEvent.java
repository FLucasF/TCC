package br.com.marealta.pousada.event;

import br.com.marealta.pousada.domain.Reservation;

/** Facts about reservations that other parts of the inn may want to react to. */
public sealed interface ReservationEvent {
    Reservation reservation();

    /** A pending reservation was paid and confirmed. */
    record Confirmed(Reservation reservation) implements ReservationEvent {
    }

    /** A reservation was cancelled; {@code wasConfirmed} tells whether it had been paid. */
    record Cancelled(Reservation reservation, boolean wasConfirmed) implements ReservationEvent {
    }
}
