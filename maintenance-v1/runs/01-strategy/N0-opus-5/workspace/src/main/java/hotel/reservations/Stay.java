package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The stay a rate plan is asked about: the room and the nights, plus the extras some plans need (the
 * company code of a corporate booking, which is null for every other plan).
 */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
