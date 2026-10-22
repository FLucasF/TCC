package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who wants to know what happens to reservations. Implement only the events you care about and
 * register the listener in {@code Hotel}; the reservation service does not need to change.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** A reservation that was already confirmed has been cancelled (the refund is on the reservation). */
    default void confirmedReservationCancelled(Reservation reservation, Room room) {
    }
}
