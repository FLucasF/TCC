package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who wants to know what happens to reservations. To tell a new person about something, write a
 * listener and register it in {@code Hotel}; the reservation service does not change.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Reservation reservation, Room room) {
    }

    /** A reservation that was already confirmed has been cancelled; its refund is set. */
    default void onConfirmedReservationCancelled(Reservation reservation, Room room) {
    }
}
