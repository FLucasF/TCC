package com.pousadamareaalta.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtils {
    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut).getDays();
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static String formatDateIso(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    public static String formatDateEnglish(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    public static YearMonth parseYearMonth(String month) {
        String[] parts = month.split("-");
        return YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public static LocalDate parseDate(String date) {
        String[] parts = date.split("-");
        return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
