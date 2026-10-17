package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class LongStayPlan implements RatePlanRules {

    @Override
    public void checkEligibility(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (ChronoUnit.DAYS.between(checkIn, checkOut) < 7) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay needs at least 7 nights");
        }
    }

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        return PriceCalculator.stay(room, checkIn, checkOut, new BigDecimal("0.10"));
    }
}
