package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * The weekend package: in on a Friday and out on the Sunday right after, exactly two nights, for a
 * closed price that is the same all year round, high season or not. Half of what was paid comes back
 * on cancellation.
 */
public final class WeekendPackagePlan implements RatePlanPolicy {

    static final int PACKAGE_NIGHTS = 2;

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.50");

    @Override
    public void validate(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != PACKAGE_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after, two nights");
        }
    }

    /** The closed price is the whole price: it is the nights and the total, and there is no discount. */
    @Override
    public Price price(Stay stay) {
        BigDecimal packagePrice = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(stay.room().type()));
        return new Price(PACKAGE_NIGHTS, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUNDED_SHARE));
    }
}
