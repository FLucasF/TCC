package com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtil {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return day >= 15;
        }

        if (month >= 1 && month <= 2) {
            return true;
        }

        return false;
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static YearMonth parseMonth(String month) {
        return YearMonth.parse(month);
    }
}
