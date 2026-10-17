package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

final class WeekendPackageStrategy implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must start on Friday");
        }
        if (request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must end on Sunday");
        }
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must be exactly 2 nights");
        }
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, NewReservation request) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.divide(new BigDecimal("2")));
    }
}
