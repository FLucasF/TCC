package hotel.reservations;

import java.time.LocalDate;
import java.util.Locale;

/** The company code only matters to the CORPORATE plan; it is kept trimmed and in capitals, or null. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {

    public NewReservation {
        companyCode = companyCode == null || companyCode.isBlank()
                ? null
                : companyCode.strip().toUpperCase(Locale.ROOT);
    }

    public NewReservation(
            int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this(roomNumber, guest, checkIn, checkOut, guests, ratePlan, null);
    }
}
