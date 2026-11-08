package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** A reservation has been cancelled. */
public record ReservationCancelledEvent(Reservation reservation, Room room, BigDecimal refund)
        implements ReservationEvent {}
