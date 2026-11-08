package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * Someone who wants to know when something happens to a reservation. Register one in the
 * {@link ReservationService} to be told; override only the events you care about.
 */
public interface ReservationListener {

    /** The guest paid and the reservation is now confirmed. */
    default void onConfirmed(Confirmed event) {
    }

    /** A reservation was cancelled. */
    default void onCancelled(Cancelled event) {
    }

    record Confirmed(Reservation reservation, Room room) {
    }

    /** {@code wasConfirmed} tells whether it had been paid and confirmed before the cancellation. */
    record Cancelled(Reservation reservation, Room room, boolean wasConfirmed, BigDecimal refund) {
    }
}
