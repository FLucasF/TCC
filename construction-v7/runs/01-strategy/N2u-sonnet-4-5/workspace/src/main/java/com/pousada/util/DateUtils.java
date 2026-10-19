package com.pousada.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static boolean isHighSeason(LocalDate date) {
        int year = date.getYear();

        LocalDate highSeasonStart = LocalDate.of(year, Month.DECEMBER, 15);
        LocalDate highSeasonEnd = LocalDate.of(year + 1, Month.FEBRUARY, 28);
        if (LocalDate.of(year + 1, 1, 1).isLeapYear()) {
            highSeasonEnd = LocalDate.of(year + 1, Month.FEBRUARY, 29);
        }

        LocalDate julyStart = LocalDate.of(year, Month.JULY, 1);
        LocalDate julyEnd = LocalDate.of(year, Month.JULY, 31);

        return (date.isAfter(highSeasonStart.minusDays(1)) && date.isBefore(highSeasonEnd.plusDays(1))) ||
               (date.isAfter(julyStart.minusDays(1)) && date.isBefore(julyEnd.plusDays(1)));
    }

    public static boolean isFriday(LocalDate date) {
        return date.getDayOfWeek().getValue() == 5;
    }

    public static boolean isSunday(LocalDate date) {
        return date.getDayOfWeek().getValue() == 7;
    }
}
