package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

final class WeekendPackagePlanStrategy implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> FIXED_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    private static final BigDecimal REFUND_PERCENTAGE = new BigDecimal("0.50");

    @Override
    public void validate(NewReservation request) {
        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must start on a Friday");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must end on a Sunday");
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package is exactly 2 nights (Friday to Sunday)");
        }
    }

    @Override
    public Price calculatePrice(Room room, NewReservation request) {
        BigDecimal fixedPrice = FIXED_PRICES.get(room.type());
        return new Price(2, fixedPrice, BigDecimal.ZERO, fixedPrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(REFUND_PERCENTAGE));
    }
}
