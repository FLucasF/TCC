package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.reservations.Reservation;

/**
 * Someone who wants to know what happens to reservations. Implement only the events you care about and
 * register the listener in {@code Hotel}; the reservation service itself never needs to change.
 */
public interface ReservationListener {

    /** A pending reservation was paid and is now confirmed. */
    default void confirmed(Reservation reservation, ReservationReceipt receipt) {
    }

    /**
     * A reservation that had been confirmed was cancelled. {@link Reservation#refund()} says how much goes
     * back to the guest (possibly zero). Reservations cancelled while still pending do not raise this.
     */
    default void confirmedReservationCancelled(Reservation reservation) {
    }
}
