package hotel.reservations;

import java.time.LocalDate;

/** {@code guests} are the adults; {@code children} (up to 12 years old) take a place but pay no nightly rate. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        int children,
        RatePlan ratePlan) {
}
