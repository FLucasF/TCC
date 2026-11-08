package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/** Weekend package: Friday to Sunday (exactly 2 nights) at a fixed price. Half refund on cancellation. */
final class WeekendPackageRatePlanStrategy implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request, Room room) {
        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must start on Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must end on Sunday");
        }
        if (!checkOut.equals(checkIn.plusDays(2))) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package is exactly 2 nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.divide(new BigDecimal("2"), PriceCalculator.ROUNDING_MODE));
    }
}
