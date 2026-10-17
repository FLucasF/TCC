package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * Friday to Sunday, exactly two nights, for a fixed price per room type all year round, season or not.
 * Half of what was paid comes back on cancellation.
 */
final class WeekendPackagePlan implements RatePlanRules {

    static final int NIGHTS = 2;
    static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.5");

    @Override
    public void checkEligibility(NewReservation request) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(NIGHTS));
        if (!fridayToSunday) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package goes from a Friday to the Sunday after it");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        BigDecimal packagePrice = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(room.type()));
        return new Price(NIGHTS, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUNDED_SHARE));
    }
}
