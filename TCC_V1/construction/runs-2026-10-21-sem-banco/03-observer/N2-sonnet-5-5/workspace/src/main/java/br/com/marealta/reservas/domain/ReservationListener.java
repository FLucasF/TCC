package br.com.marealta.reservas.domain;

/** Anyone who needs to know about what happens to reservations. Implement only the events of interest. */
public interface ReservationListener {

    default void onConfirmed(Reservation reservation) {}

    /** {@code previousStatus} is the status the reservation had right before the cancellation. */
    default void onCancelled(Reservation reservation, ReservationStatus previousStatus) {}
}
