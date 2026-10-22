package hotel.reservations;

import java.time.LocalDate;

/** {@code companyCode} is only used by the corporate rate plan; it is null when the guest gave none. */
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
