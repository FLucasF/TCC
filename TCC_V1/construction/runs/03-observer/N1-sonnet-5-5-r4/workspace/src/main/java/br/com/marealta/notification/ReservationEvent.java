package br.com.marealta.notification;

import br.com.marealta.domain.Reservation;

/** Algo que aconteceu com uma reserva e que alguém, além do hóspede, pode precisar saber. */
public sealed interface ReservationEvent {
    Reservation reservation();

    /** A reserva foi paga e confirmada. */
    record Confirmed(Reservation reservation) implements ReservationEvent {
    }

    /** Uma reserva que já estava confirmada foi cancelada (reserva pendente cancelada não gera evento). */
    record Cancelled(Reservation reservation) implements ReservationEvent {
    }
}
