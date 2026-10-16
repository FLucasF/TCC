package hotel.reservations;

import java.time.LocalDate;

/** A reservation to be made. The {@code companyCode} only means something to the corporate plan. */
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
