package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Check-in on a Friday and check-out on the Sunday after it, for a closed price by room type. */
final class WeekendPackageRule implements RatePlanRule {

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public Price price(Room room, NewReservation request, Price seasonal) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the rate plan WEEKEND_PACKAGE is from a Friday to the Sunday after it");
        }
        BigDecimal packagePrice = PRICE_BY_ROOM_TYPE.get(room.type());
        return new Price(seasonal.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUND_RATE));
    }
}
