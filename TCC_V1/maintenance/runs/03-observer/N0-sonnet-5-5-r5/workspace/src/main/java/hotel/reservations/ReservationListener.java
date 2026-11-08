package hotel.reservations;

/**
 * Someone who wants to know what happens to reservations. Implement only the events of interest and
 * register the listener where the service is built.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Reservation reservation) {
    }

    /**
     * The reservation was cancelled. {@code wasConfirmed} tells whether it had been paid for (confirmed)
     * before; the refund is in {@link Reservation#refund()}.
     */
    default void onCancelled(Reservation reservation, boolean wasConfirmed) {
    }
}
