package hotel.reservations;

import java.time.LocalDate;

/** {@code companyCode} is only used by the CORPORATE plan, and may be null. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
