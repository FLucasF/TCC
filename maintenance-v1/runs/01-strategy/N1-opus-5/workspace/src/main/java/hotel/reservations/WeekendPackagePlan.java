package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * The weekend package: in on a Friday, out on the Sunday right after, two nights at a closed price
 * that holds all year round, high season or not. Half of what was paid comes back on cancellation.
 */
final class WeekendPackagePlan implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.50");

    @Override
    public void checkBookable(NewReservation request) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after it");
        }
    }

    @Override
    public Price price(Stay stay) {
        return Price.closed(stay.nights(), PRICE_BY_ROOM_TYPE.get(stay.room().type()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUNDED_SHARE));
    }
}
