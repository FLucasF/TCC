package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** What a rate plan needs to know about a stay to decide if it applies and what it costs. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
