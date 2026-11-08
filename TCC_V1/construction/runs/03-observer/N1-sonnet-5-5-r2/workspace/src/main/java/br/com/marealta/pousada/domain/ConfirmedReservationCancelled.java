package br.com.marealta.pousada.domain;

/** Uma reserva que já estava confirmada (paga) foi cancelada. O reembolso está em reservation.refund(). */
public record ConfirmedReservationCancelled(Reservation reservation) {
}
