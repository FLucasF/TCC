package hotel.reservations.plans;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * The weekend package: Friday check-in, Sunday check-out (exactly two nights), at a flat price per
 * room type that holds all year. Cancelling gives half of what was paid back.
 */
public final class WeekendPackagePolicy implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal HALF = new BigDecimal("0.5");

    @Override
    public void validate(NewReservation request) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                || request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY
                || ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PricingSupport pricing) {
        BigDecimal packagePrice = pricing.cents(PACKAGE_PRICE.get(room.type()));
        return new Price(2, packagePrice, pricing.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid.multiply(HALF).setScale(2, RoundingMode.HALF_EVEN);
    }
}
