package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class StandardPlan implements RatePlanRules {

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        return PriceCalculator.stay(room, checkIn, checkOut, BigDecimal.ZERO);
    }
}
