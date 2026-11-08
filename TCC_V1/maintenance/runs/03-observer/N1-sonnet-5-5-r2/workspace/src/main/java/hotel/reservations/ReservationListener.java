package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who wants to know what happens to reservations. Each listener picks the events it cares
 * about; the others do nothing.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** A reservation that was already confirmed has been cancelled; its refund is set. */
    default void confirmedReservationCancelled(Reservation reservation, Room room) {
    }
}
