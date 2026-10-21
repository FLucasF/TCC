package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Reservation;

/** Facts about reservations that other parts of the inn may want to hear about. */
public sealed interface ReservationEvent {
    Reservation reservation();

    /** The guest paid and the reservation became CONFIRMED. */
    record Confirmed(Reservation reservation) implements ReservationEvent {
    }

    /** A reservation that had been confirmed (paid) was cancelled; its refund is already set. */
    record CancelledAfterConfirmation(Reservation reservation) implements ReservationEvent {
    }
}
