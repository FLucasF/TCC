package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * Someone who wants to know what happens to reservations (the housekeeping team, finance, the
 * restaurant...). Implement only the events of interest; adding a listener never touches the service.
 */
public interface ReservationListener {

    /** A pending reservation was paid and is now confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /**
     * A confirmed (paid) reservation was cancelled; {@code refund} is what goes back to the guest, possibly
     * zero. Cancelling a reservation that was never confirmed does not raise this event.
     */
    default void cancelledAfterConfirmation(Reservation reservation, Room room, BigDecimal refund) {
    }
}
