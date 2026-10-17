package hotel.reservations;

import java.time.LocalDate;

/** {@code companyCode} is only for the CORPORATE rate plan; the other plans ignore it (it may be null). */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
