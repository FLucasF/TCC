package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/** Friday to Sunday, two nights, at a closed price all year round; half comes back on cancellation. */
final class WeekendPackageRule implements RatePlanRule {

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUND_SHARE = new BigDecimal("0.5");

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price standardPrice) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday after it");
        }
        BigDecimal packagePrice = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(room.type()));
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUND_SHARE));
    }
}
