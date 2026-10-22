package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class PriceCalculator {

    public Price calculate(
            Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        return ratePlan.pricing().calculatePrice(room, checkIn, checkOut, companyCode);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
