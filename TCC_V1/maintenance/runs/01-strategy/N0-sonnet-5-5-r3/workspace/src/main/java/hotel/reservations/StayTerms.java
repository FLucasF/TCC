package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** What a rate plan needs to know about a stay to accept it and price it. */
public record StayTerms(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
