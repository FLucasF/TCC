package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** The stay a rate plan judges and prices: the room, the nights, and the data the plan needs. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
