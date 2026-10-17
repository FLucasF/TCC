package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * From a Friday to the Sunday right after, for a fixed price by room type, all year round. Half of what
 * was paid comes back on cancellation.
 */
final class WeekendPackageRate implements RatePlanPolicy {

    static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    static final int NIGHTS = 2;

    @Override
    public void checkEligibility(NewReservation request) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(NIGHTS));
        if (!fridayToSunday) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the WEEKEND_PACKAGE rate plan goes from a Friday to the next Sunday");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        BigDecimal packagePrice = PRICE_BY_ROOM_TYPE.get(room.type());
        return new Price(NIGHTS, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid().divide(BigDecimal.TWO, 2, RoundingMode.HALF_EVEN);
    }
}
