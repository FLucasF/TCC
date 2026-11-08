package com.pousada.util;

import java.time.LocalDate;
import java.time.Month;

public class DateUtil {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER) {
            return dayOfMonth >= 15;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static int getNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() == 5; // Friday
    }

    public static LocalDate nextSunday(LocalDate friday) {
        return friday.plusDays(2);
    }
}
