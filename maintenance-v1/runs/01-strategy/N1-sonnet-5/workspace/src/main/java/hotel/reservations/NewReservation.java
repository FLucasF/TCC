package hotel.reservations;

import java.time.LocalDate;

/** {@code companyCode} is only used by the CORPORATE rate plan; other plans ignore it. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
