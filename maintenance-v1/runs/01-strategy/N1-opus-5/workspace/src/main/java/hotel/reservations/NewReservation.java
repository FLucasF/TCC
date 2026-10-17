package hotel.reservations;

import java.time.LocalDate;

/** What the guest asks for. The company code only means something to the corporate plan. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
