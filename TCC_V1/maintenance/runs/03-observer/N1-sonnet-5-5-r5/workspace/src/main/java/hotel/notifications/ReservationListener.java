package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Someone who wants to know what happens to reservations. Each one reacts only to what interests it. */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** The reservation was cancelled; {@code wasConfirmed} tells whether it had been paid for. */
    default void cancelled(Reservation reservation, Room room, boolean wasConfirmed) {
    }
}
