package hotel.reservations;

import hotel.rooms.Room;

/** Reacts to what happens to a reservation. Each listener implements only the events it cares about. */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Reservation reservation, Room room) {
    }

    /** The reservation was cancelled; {@code previousStatus} is what it was before (pending or confirmed). */
    default void onCancelled(Reservation reservation, Room room, ReservationStatus previousStatus) {
    }
}
