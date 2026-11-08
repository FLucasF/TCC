package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who wants to know what happens to reservations. Each listener picks the events it cares about;
 * a new interested party is a new listener, with no change to {@link ReservationService}.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** The reservation was cancelled; {@code wasConfirmed} tells whether it had already been paid. */
    default void cancelled(Reservation reservation, Room room, boolean wasConfirmed) {
    }
}
