package com.pousada;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public static LocalDate parseIsoDate(String date) {
        return LocalDate.parse(date, ISO_DATE);
    }

    public static YearMonth parseYearMonth(String month) {
        return YearMonth.parse(month, YEAR_MONTH);
    }

    public static String formatDisplay(LocalDate date) {
        return date.format(DISPLAY_DATE);
    }

    public static String formatIso(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
