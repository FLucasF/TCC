package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;

/**
 * What a rate plan needs to judge a stay and put a price on it: the room, the dates, and the company
 * code (only the corporate plan reads it; the others ignore it, so it may be null).
 */
public record PricingContext(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
}
