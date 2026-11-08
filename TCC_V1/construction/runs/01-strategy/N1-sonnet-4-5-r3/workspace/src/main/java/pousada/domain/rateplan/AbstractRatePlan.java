package pousada.domain.rateplan;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

public abstract class AbstractRatePlan implements RatePlan {

    protected static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    protected BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    protected boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    protected BigDecimal calculateNightPrice(LocalDate night, RoomType roomType) {
        BigDecimal baseRate = roomType.nightlyRate();
        if (isHighSeason(night)) {
            return round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    protected BigDecimal calculateSubtotalForNights(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightPrice(current, roomType));
            current = current.plusDays(1);
        }

        return round(subtotal);
    }

    protected long countNights(LocalDate checkIn, LocalDate checkOut) {
        return checkOut.toEpochDay() - checkIn.toEpochDay();
    }

    protected boolean isWeekendPackagePeriod(LocalDate checkIn, LocalDate checkOut) {
        return checkIn.getDayOfWeek() == DayOfWeek.FRIDAY &&
               checkOut.getDayOfWeek() == DayOfWeek.SUNDAY &&
               countNights(checkIn, checkOut) == 2;
    }
}
