package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

import static hotel.reservations.PriceCalculator.cents;

/** Fixed-price weekend package (Friday to Sunday); 50% refund on cancellation. */
final class WeekendPackageRatePlan extends BaseRatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package requires check-in on Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package requires check-out on Sunday");
        }
        int nights = countNights(checkIn, checkOut);
        if (nights != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "weekend package is exactly 2 nights (Friday to Sunday)");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        int nights = 2;
        // For weekend package, the package price is both the subtotal and the total (no discount)
        return new Price(nights, packagePrice, cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return cents(amountPaid.multiply(new BigDecimal("0.50")));
    }
}
