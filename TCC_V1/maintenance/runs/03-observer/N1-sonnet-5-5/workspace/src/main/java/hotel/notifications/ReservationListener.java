package hotel.notifications;

import hotel.reservations.Reservation;

/**
 * Someone who wants to know what happens to reservations. Each listener picks the events it cares about; a new
 * party to notify is a new listener, plugged in where the inn is put together.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void confirmed(Reservation reservation) {
    }

    /** A reservation that was already confirmed has been cancelled (its refund is set). */
    default void cancelledAfterConfirmation(Reservation reservation) {
    }
}
