package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Map;

/** Friday to Sunday, two nights, at a closed price all year round; half comes back on cancellation. */
final class WeekendPackagePolicy implements RatePlanPolicy {

    static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public Price price(Stay stay, Price fullPrice) {
        boolean isWeekend = stay.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && stay.checkOut().equals(stay.checkIn().plusDays(2));
        if (!isWeekend) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday after it");
        }
        BigDecimal packagePrice = PRICES.get(stay.room().type());
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid.multiply(REFUND_RATE).setScale(2, RoundingMode.HALF_EVEN);
    }
}
