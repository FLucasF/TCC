package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

final class WeekendPackageStrategy implements RatePlanStrategy {

    private static final java.util.Map<RoomType, BigDecimal> WEEKEND_PRICES = java.util.Map.ofEntries(
            java.util.Map.entry(RoomType.STANDARD, new BigDecimal("520.00")),
            java.util.Map.entry(RoomType.DELUXE, new BigDecimal("820.00")),
            java.util.Map.entry(RoomType.SUITE, new BigDecimal("1200.00")));

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut) {
        validate(checkIn, checkOut, 0);
        BigDecimal fixedPrice = WEEKEND_PRICES.get(room.type());
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(2, fixedPrice, discount, fixedPrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights) {
        DayOfWeek checkInDay = checkIn.getDayOfWeek();
        DayOfWeek checkOutDay = checkOut.getDayOfWeek();
        int daysDiff = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        if (checkInDay != DayOfWeek.FRIDAY || checkOutDay != DayOfWeek.SUNDAY || daysDiff != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package requires check-in on Friday and check-out on Sunday (2 nights)");
        }
    }
}
