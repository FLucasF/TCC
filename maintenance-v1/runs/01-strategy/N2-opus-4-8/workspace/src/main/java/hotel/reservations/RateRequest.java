package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;

/**
 * What a rate plan needs to know about a stay to decide whether it may be used and what it costs.
 * A new plan that looks at something else adds its field here, in one place.
 */
public record RateRequest(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
}
