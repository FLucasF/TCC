package hotel.reservations;

import hotel.rooms.Room;

/**
 * Someone who wants to know what happens to reservations. Add a listener to the service to be told;
 * new kinds of news become new default methods here, so existing listeners are not affected.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Reservation reservation, Room room) {
    }

    /** A confirmed (paid) reservation was cancelled; {@code reservation.refund()} says what goes back. */
    default void onConfirmedReservationCancelled(Reservation reservation, Room room) {
    }
}
