package hotel.reservations;

import java.time.LocalDate;

public record NewReservation(
        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, int children, RatePlan ratePlan) {

    /** A reservation with no children. */
    public NewReservation(
            int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this(roomNumber, guest, checkIn, checkOut, guests, 0, ratePlan);
    }
}
