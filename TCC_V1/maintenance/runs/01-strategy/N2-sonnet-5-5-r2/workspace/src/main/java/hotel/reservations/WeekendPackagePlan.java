package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Friday to Sunday, two nights, for a closed price that ignores the season. */
final class WeekendPackagePlan implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    private static final BigDecimal REFUND = new BigDecimal("0.50");

    @Override
    public Price price(Room room, NewReservation request, Price base) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                || request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY
                || base.nights() != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday right after");
        }
        BigDecimal price = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(room.type()));
        return new Price(base.nights(), price, PriceCalculator.cents(BigDecimal.ZERO), price);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return RatePlanPolicy.percentOf(amountPaid, REFUND);
    }
}
