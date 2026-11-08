package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;

/** Fatos da reserva que interessam a quem precisa ser avisado. */
public sealed interface ReservationEvent {

    Reservation reservation();

    Room room();

    record Confirmed(Reservation reservation, Room room) implements ReservationEvent {
    }

    /** {@code wasConfirmed}: a reserva já estava confirmada (paga) quando foi cancelada. */
    record Cancelled(Reservation reservation, Room room, boolean wasConfirmed) implements ReservationEvent {
    }
}
