package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * The weekend package: check in on a Friday, check out on the Sunday right after (exactly two nights),
 * for a flat price that holds all year, high season or not. Any other dates cannot take the package.
 * The flat price is the subtotal and the total, with no discount. Cancelling gives back half of what
 * was paid.
 */
final class WeekendPackagePolicy implements RatePlanPolicy {

    private static final int PACKAGE_NIGHTS = 2;
    private static final BigDecimal REFUND_SHARE = new BigDecimal("0.5");

    /** The flat package price by room type. */
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(PACKAGE_NIGHTS))) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package runs from a Friday to the Sunday right after, two nights");
        }
    }

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator pricing) {
        BigDecimal packagePrice = PriceCalculator.cents(PACKAGE_PRICES.get(room.type()));
        return new Price(PACKAGE_NIGHTS, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUND_SHARE));
    }
}
