package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;

/** What a rate plan needs to know about a requested stay; the company code only matters to some plans. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
}
