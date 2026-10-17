package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * From Friday to the Sunday right after, for a fixed price per room type, all year round. Half of what
 * was paid comes back on cancellation.
 */
final class WeekendPackagePlan implements RatePlanRules {

    static final Map<RoomType, BigDecimal> PACKAGE_PRICE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.50");

    @Override
    public void checkEligibility(NewReservation request) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the WEEKEND_PACKAGE plan goes from a Friday to the Sunday after");
        }
    }

    @Override
    public Price price(NewReservation request, Room room, Price seasonalPrice) {
        BigDecimal packagePrice = PriceCalculator.cents(PACKAGE_PRICE.get(room.type()));
        return new Price(seasonalPrice.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUNDED_SHARE));
    }
}
