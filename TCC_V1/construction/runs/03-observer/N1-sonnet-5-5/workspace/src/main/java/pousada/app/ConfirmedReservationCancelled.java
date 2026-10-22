package pousada.app;

import pousada.domain.Reservation;

/** Cancelamento de uma reserva que já estava confirmada (cancelada, com o reembolso já definido). */
public record ConfirmedReservationCancelled(Reservation reservation) {
}
