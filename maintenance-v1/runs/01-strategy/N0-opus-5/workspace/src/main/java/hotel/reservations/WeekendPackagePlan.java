package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * The weekend package: in on a Friday and out on the Sunday right after, two nights for a closed
 * price that is the same all year, high season or not. Cancelling gives half of what was paid back.
 */
final class WeekendPackagePlan implements RatePlanPolicy {

    static final int NIGHTS = 2;
    static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.50");

    /** What the package costs, by room type, whatever the dates. */
    static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void checkEligibility(Stay stay) {
        boolean fridayToSunday = stay.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && stay.checkOut().getDayOfWeek() == DayOfWeek.SUNDAY
                && stay.nights() == NIGHTS;
        if (!fridayToSunday) {
            throw RatePlanPolicy.notEligible(
                    "the weekend package goes from a Friday to the Sunday right after");
        }
    }

    /** The closed price: it is the nights and the total at once, with no discount on top. */
    @Override
    public Price price(Stay stay) {
        BigDecimal packagePrice = PriceCalculator.cents(PACKAGE_PRICES.get(stay.room().type()));
        return new Price(stay.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUNDED_SHARE));
    }
}
