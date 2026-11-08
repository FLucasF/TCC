package hotel.reservations;

import hotel.rooms.Room;

/** Someone who reacts to what happens to a reservation. Each listener decides which events matter to it. */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** The reservation was cancelled; {@code statusBefore} tells whether it had been confirmed. */
    default void cancelled(Reservation reservation, Room room, ReservationStatus statusBefore) {
    }
}
