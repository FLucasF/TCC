package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

final class WeekendPackagePlan implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package requires check-in on Friday");
        }
        if (request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package requires check-out on Sunday");
        }
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights != 2) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package is exactly 2 nights (Friday to Sunday)");
        }
    }

    @Override
    public BigDecimal calculateDiscount(
            BigDecimal subtotal, NewReservation request, Room room, LocalDate checkIn, LocalDate checkOut) {
        // Package has fixed price, so discount = subtotal - package price
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        return PriceCalculator.cents(subtotal.subtract(packagePrice));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.divide(new BigDecimal("2")));
    }
}
