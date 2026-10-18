package hotel.reservations;

import hotel.rooms.Room;

/** Someone who wants to know what happens to reservations. Every method does nothing unless overridden. */
public interface ReservationListener {

    /** The guest paid and the reservation was confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** A reservation that had been confirmed was cancelled; the refund is already set on it. */
    default void confirmedReservationCancelled(Reservation reservation, Room room) {
    }
}
