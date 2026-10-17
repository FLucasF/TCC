package hotel.reservations;

import java.time.LocalDate;

/**
 * A reservation the website asks for. {@code companyCode} serves only the corporate rate; on the
 * other plans it may be left out (and is ignored if it comes).
 */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
