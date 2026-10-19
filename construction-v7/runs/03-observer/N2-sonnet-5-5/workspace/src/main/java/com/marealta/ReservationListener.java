package com.marealta;

/**
 * Observador dos eventos da reserva. Quem quiser ser avisado (restaurante, transfer...)
 * implementa esta interface e é registrado em {@link App}; o serviço de reservas não muda.
 */
public interface ReservationListener {
    /** A reserva acabou de ser confirmada (paga). */
    default void onConfirmed(Reservation reservation) {}

    /** Uma reserva que já estava confirmada acabou de ser cancelada. */
    default void onPaidReservationCancelled(Reservation reservation) {}
}
