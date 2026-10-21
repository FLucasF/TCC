package hotel.reservations;

import hotel.rooms.Room;

/** Reacts to what happens to a reservation. Each listener acts only on the events it cares about. */
public interface ReservationListener {

    /** The guest paid and the reservation was confirmed. */
    default void onConfirmed(Reservation reservation, Room room) {
    }

    /** The reservation was cancelled; {@code wasConfirmed} tells whether it had been paid before. */
    default void onCancelled(Reservation reservation, Room room, boolean wasConfirmed) {
    }
}
