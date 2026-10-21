package br.com.marealta.notification;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

/** Fatos que acontecem com uma reserva e que interessam a quem quer ser avisado. */
public sealed interface ReservationEvent {

    Reservation reservation();

    record Confirmed(Reservation reservation) implements ReservationEvent {
    }

    /** {@code previousStatus} é a situação da reserva antes de ser cancelada. */
    record Cancelled(Reservation reservation, ReservationStatus previousStatus) implements ReservationEvent {
    }
}
