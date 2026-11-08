package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

    public Price calculate(Room room, ReservationData data, RatePlan ratePlan) {
        return ratePlan.strategy().calculate(room, data);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
