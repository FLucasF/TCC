package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** Something that happened to a reservation that others in the inn may want to know about. */
public sealed interface ReservationEvent {

    Reservation reservation();

    Room room();

    /** The guest paid and the reservation was confirmed. */
    record Confirmed(Reservation reservation, Room room) implements ReservationEvent {
    }

    /** A confirmed (paid) reservation was cancelled; {@code refund} is what goes back to the guest. */
    record CancelledAfterPayment(Reservation reservation, Room room, BigDecimal refund)
            implements ReservationEvent {
    }
}
