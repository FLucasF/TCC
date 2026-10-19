package com.pousada.utils;

import java.time.LocalDate;
import java.time.Month;

public class DateUtils {
    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return checkOut.toEpochDay() - checkIn.toEpochDay();
    }

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.DECEMBER && dayOfMonth >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.JULY) {
            return true;
        }

        return false;
    }

    public static boolean dateIsInRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && date.isBefore(end);
    }

    public static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static String formatDateISO(LocalDate date) {
        return date.toString();
    }

    public static String formatYearMonth(LocalDate date) {
        return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
    }
}
