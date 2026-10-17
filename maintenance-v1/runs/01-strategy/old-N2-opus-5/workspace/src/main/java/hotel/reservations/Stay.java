package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A stay as the rate plans see it: the room, the nights, and the data a plan asks the guest for. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
