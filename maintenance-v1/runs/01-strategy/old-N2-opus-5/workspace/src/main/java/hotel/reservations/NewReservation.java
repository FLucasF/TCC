package hotel.reservations;

import java.time.LocalDate;

/** A reservation asked for. The company code only serves the plans that ask for one. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
