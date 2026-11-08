package br.com.pousada.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate parseIsoDate(String dateStr) {
        return LocalDate.parse(dateStr, ISO_DATE);
    }

    public static String formatBrDate(LocalDate date) {
        return date.format(BR_DATE);
    }

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static YearMonth parseYearMonth(String yearMonthStr) {
        return YearMonth.parse(yearMonthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        int year = date.getYear();

        // Julho inteiro
        if (month == 7) return true;

        // 15 de dezembro a 28 de fevereiro
        LocalDate dateToCheck = LocalDate.of(year, month, day);
        LocalDate seasonStart = LocalDate.of(year, 12, 15);
        LocalDate seasonEnd = LocalDate.of(year + 1, 2, 28);

        if (dateToCheck.isBefore(seasonStart)) {
            // Se é antes de 15 de dezembro, verifica se é do ano anterior
            seasonStart = LocalDate.of(year - 1, 12, 15);
            seasonEnd = LocalDate.of(year, 2, 28);
        }

        return !dateToCheck.isBefore(seasonStart) && !dateToCheck.isAfter(seasonEnd);
    }
}
