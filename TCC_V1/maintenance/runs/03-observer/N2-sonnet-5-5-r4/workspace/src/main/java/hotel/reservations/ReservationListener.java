package hotel.reservations;

/** Someone who reacts to what happens to a reservation. Each listener decides what, if anything, to do. */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation) {
    }

    /** The reservation was cancelled; {@code previousStatus} says whether it had been confirmed. */
    default void cancelled(Reservation reservation, ReservationStatus previousStatus) {
    }
}
