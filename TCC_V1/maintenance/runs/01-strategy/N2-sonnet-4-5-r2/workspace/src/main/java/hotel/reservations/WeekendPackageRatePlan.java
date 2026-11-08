package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/** Weekend package: Friday to Sunday, fixed price per room type, available year-round. */
public final class WeekendPackageRatePlan implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package must start on Friday");
        }
        if (nights != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package is exactly two nights (Friday to Sunday)");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
        int nights = calculator.countNights(checkIn, checkOut);
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, packagePrice, discount, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.divide(new BigDecimal("2")));
    }
}
