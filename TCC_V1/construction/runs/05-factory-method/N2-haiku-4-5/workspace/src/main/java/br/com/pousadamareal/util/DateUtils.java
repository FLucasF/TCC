package br.com.pousadamareal.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter BRAZILIAN_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ENGLISH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String toBrazilian(LocalDate date) {
        return date.format(BRAZILIAN_FORMAT);
    }

    public static String toEnglish(LocalDate date) {
        return date.format(ENGLISH_FORMAT);
    }

    public static LocalDate fromISO(String date) {
        return LocalDate.parse(date, ISO_FORMAT);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
