package hotel.reservations;

import java.time.LocalDate;
import java.util.Optional;

public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        Optional<String> companyCode) {

    public NewReservation(int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this(roomNumber, guest, checkIn, checkOut, guests, ratePlan, Optional.empty());
    }
}
