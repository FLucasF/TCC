package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

final class WeekendPackagePlan implements RatePlanRules {

    @Override
    public void checkEligibility(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package is two nights, from a Friday to the Sunday");
        }
    }

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal packagePrice = switch (room.type()) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
        };
        return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
    }
}
