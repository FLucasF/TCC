package hotel.reservations;

import java.time.LocalDate;

/**
 * A reservation to create. {@code companyCode} only matters to the corporate rate; the other plans
 * ignore it, so it may be null.
 */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {

    public NewReservation(
            int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this(roomNumber, guest, checkIn, checkOut, guests, ratePlan, null);
    }
}
