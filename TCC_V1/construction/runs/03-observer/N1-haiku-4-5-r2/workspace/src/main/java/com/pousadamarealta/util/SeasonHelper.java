package com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.Month;

public class SeasonHelper {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == Month.JULY.getValue()) return true;

        if (month == Month.DECEMBER.getValue() && day >= 15) return true;

        if (month == Month.JANUARY.getValue()) return true;
        if (month == Month.FEBRUARY.getValue()) return true;

        return false;
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
