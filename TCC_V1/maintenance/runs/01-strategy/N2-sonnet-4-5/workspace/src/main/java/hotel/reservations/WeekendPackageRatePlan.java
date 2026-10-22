package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

final class WeekendPackageRatePlan implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    private static final int PACKAGE_NIGHTS = 2;

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights != PACKAGE_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package requires exactly " + PACKAGE_NIGHTS + " nights");
        }
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must start on a Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must end on a Sunday");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, packagePrice, discount, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.divide(new BigDecimal("2"), PriceCalculator.cents(BigDecimal.ZERO).scale(), java.math.RoundingMode.HALF_EVEN));
    }
}
