package br.com.mareaalta.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static LocalDate parse(String dateStr) {
        try {
            return LocalDate.parse(dateStr, ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    public static String format(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static String formatBr(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static String formatYearMonth(YearMonth ym) {
        return String.format("%04d-%02d", ym.getYear(), ym.getMonthValue());
    }

    public static YearMonth parseYearMonth(String str) {
        try {
            String[] parts = str.split("-");
            if (parts.length != 2) return null;
            return YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }

    public static int nightsBetween(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month <= 2) return true;

        return false;
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static boolean isFriday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    public static boolean isSunday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static LocalDate nextSunday(LocalDate friday) {
        return friday.plusDays(2);
    }
}
