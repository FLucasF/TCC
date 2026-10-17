package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

final class WeekendPackagePolicy implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY
                || PriceCalculator.countNights(checkIn, checkOut) != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package requires check-in on Friday and check-out on Sunday");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUND_RATE));
    }
}
