package hotel.reservations;

import java.time.LocalDate;

/** A reservation the website asks for. {@code companyCode} only means something to the corporate plan. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
