package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who reacts to what happens to a reservation. Each listener implements only the events it cares
 * about and decides for itself whether to act.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Reservation reservation, Room room) {
    }

    /** A confirmed reservation was cancelled (a reservation that was never confirmed announces nothing). */
    default void onCancelled(Reservation reservation, Room room) {
    }
}
