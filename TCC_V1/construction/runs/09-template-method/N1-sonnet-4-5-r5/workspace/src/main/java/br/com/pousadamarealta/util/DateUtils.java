package br.com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatDisplay(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    public static String formatIso(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == Month.JULY) {
            return true;
        }

        // 15 de dezembro até fim de fevereiro
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }
}
