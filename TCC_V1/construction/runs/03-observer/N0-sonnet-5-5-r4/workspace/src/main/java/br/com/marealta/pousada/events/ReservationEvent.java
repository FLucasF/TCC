package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

/** Fatos que acontecem com uma reserva e que outras áreas podem querer saber. */
public sealed interface ReservationEvent {

    Reservation reservation();

    Room room();

    record Confirmed(Reservation reservation, Room room) implements ReservationEvent {
    }

    /** {@code wasPaid}: a reserva já estava confirmada (paga) quando foi cancelada. */
    record Cancelled(Reservation reservation, Room room, boolean wasPaid) implements ReservationEvent {
    }
}
