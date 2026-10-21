package hotel.reservations;

/** Something that happened to a reservation, for whoever needs to know about it. */
public sealed interface ReservationEvent {

    Reservation reservation();

    /** The guest paid and the reservation is now confirmed. */
    record Confirmed(Reservation reservation) implements ReservationEvent {
    }

    /** The reservation was cancelled; {@code wasConfirmed} tells whether it had been paid for. */
    record Cancelled(Reservation reservation, boolean wasConfirmed) implements ReservationEvent {
    }
}
