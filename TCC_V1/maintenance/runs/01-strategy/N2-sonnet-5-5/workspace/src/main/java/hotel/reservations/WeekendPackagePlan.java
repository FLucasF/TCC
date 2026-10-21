package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Friday to Sunday (two nights) for a closed price all year round; half comes back on cancellation. */
final class WeekendPackagePlan implements RatePlanPolicy {

    private static final int NIGHTS = 2;
    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                || !request.checkOut().equals(request.checkIn().plusDays(NIGHTS))) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the WEEKEND_PACKAGE plan is from a Friday to the following Sunday");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        BigDecimal packagePrice = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(room.type()));
        return new Price(NIGHTS, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
    }
}
