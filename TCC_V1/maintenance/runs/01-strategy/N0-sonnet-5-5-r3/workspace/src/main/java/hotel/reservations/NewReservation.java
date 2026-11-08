package hotel.reservations;

import java.time.LocalDate;

public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {

    /** The company code only means something to the corporate plan; the other plans drop it. */
    public NewReservation {
        if (ratePlan != RatePlan.CORPORATE) {
            companyCode = null;
        }
    }

    public NewReservation(
            int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this(roomNumber, guest, checkIn, checkOut, guests, ratePlan, null);
    }
}
