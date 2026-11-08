package br.com.marealta.events;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;

/** Fatos que acontecem com uma reserva e que outras áreas podem querer saber. */
public sealed interface ReservationEvent {
    Reservation reservation();

    Room room();

    record Confirmed(Reservation reservation, Room room) implements ReservationEvent {
    }

    record Cancelled(Reservation reservation, Room room, ReservationStatus previousStatus)
            implements ReservationEvent {
        /** A reserva já estava paga quando foi cancelada? */
        public boolean wasConfirmed() {
            return previousStatus == ReservationStatus.CONFIRMED;
        }
    }
}
