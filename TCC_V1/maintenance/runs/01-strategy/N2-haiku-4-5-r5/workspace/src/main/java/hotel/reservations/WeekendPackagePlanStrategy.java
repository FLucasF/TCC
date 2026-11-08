package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class WeekendPackagePlanStrategy implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> WEEKEND_PRICES = new HashMap<>();

    static {
        WEEKEND_PRICES.put(RoomType.STANDARD, new BigDecimal("520.00"));
        WEEKEND_PRICES.put(RoomType.DELUXE, new BigDecimal("820.00"));
        WEEKEND_PRICES.put(RoomType.SUITE, new BigDecimal("1200.00"));
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, Optional<String> companyCode) {
        if (nights != 2) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must be exactly 2 nights");
        }
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must start on a Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must end on a Sunday");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, Optional<String> companyCode) {
        BigDecimal packagePrice = WEEKEND_PRICES.get(room.type());
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(Price price, ReservationStatus status) {
        return PriceCalculator.cents(price.total().divide(new BigDecimal("2")));
    }
}
