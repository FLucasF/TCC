package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * The weekend package: in on Friday, out on the Sunday right after, exactly two nights, at a closed
 * price all year round. Half of what was paid comes back on cancellation.
 */
final class WeekendPackagePlan implements RatePlanPolicy {

    private static final int NIGHTS = 2;
    private static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.50");

    /** The closed price of the package, which does not change with the season. */
    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void checkEligibility(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after");
        }
    }

    @Override
    public Price price(Stay stay) {
        BigDecimal packagePrice = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(stay.room().type()));
        return new Price(stay.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUNDED_SHARE));
    }
}
