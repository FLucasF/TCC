package hotel.reservations;

import java.time.LocalDate;

/** @param companyCode the company of a CORPORATE reservation; null when the plan does not use it */
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
