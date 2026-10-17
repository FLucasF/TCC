package hotel.reservations;

import java.time.LocalDate;

/** A reservation to be made. The company code is only used by the corporate rate; it may be null. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
