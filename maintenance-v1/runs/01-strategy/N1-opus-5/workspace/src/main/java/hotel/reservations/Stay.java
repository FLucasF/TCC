package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * A stay ready to be priced: the room, what the guest asked for, and what the nights cost before the
 * rate plan has its say.
 */
public record Stay(Room room, NewReservation request, int nights, BigDecimal nightsSubtotal) {
}
