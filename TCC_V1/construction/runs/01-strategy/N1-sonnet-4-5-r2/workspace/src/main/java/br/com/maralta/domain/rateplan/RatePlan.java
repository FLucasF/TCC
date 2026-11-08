package br.com.maralta.domain.rateplan;

import br.com.maralta.domain.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public interface RatePlan {
    void validate(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType, String companyCode);

    BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType);

    BigDecimal calculateDiscount(BigDecimal subtotal);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    default boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    default boolean isFriday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    default boolean isSunday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
