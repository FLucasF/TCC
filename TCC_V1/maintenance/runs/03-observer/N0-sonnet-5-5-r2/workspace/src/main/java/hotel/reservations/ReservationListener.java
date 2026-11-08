package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who wants to hear about what happens to reservations. Implement only the events of interest;
 * the service tells every registered listener, so a new interested party needs no change to the service.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Reservation reservation, Room room) {
    }

    /** A reservation that had been confirmed was cancelled; its refund is already set. */
    default void onConfirmedReservationCancelled(Reservation reservation, Room room) {
    }
}
