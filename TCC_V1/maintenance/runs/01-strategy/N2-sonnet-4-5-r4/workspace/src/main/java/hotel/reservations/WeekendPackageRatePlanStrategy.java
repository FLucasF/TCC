package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

final class WeekendPackageRatePlanStrategy implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(NewReservation request, Room room) {
        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();

        long nights = checkIn.until(checkOut).getDays();
        if (nights != 2) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package requires exactly 2 nights");
        }

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package requires check-in on Friday and check-out on Sunday");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.divide(new BigDecimal("2")));
    }
}
