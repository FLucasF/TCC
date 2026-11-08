package br.marealta.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatters {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DATE_FORMAT);
    }

    public static String formatMoney(BigDecimal value) {
        if (value == null) return null;
        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(BR_LOCALE));
        return "R$ " + df.format(value);
    }

    public static LocalDate parseIsoDate(String date) {
        return LocalDate.parse(date, ISO_DATE_FORMAT);
    }

    public static String formatMoneyForCsv(BigDecimal value) {
        if (value == null) return null;
        return String.format("%.2f", value);
    }

    public static String formatDateForCsv(LocalDate date) {
        if (date == null) return null;
        return date.format(ISO_DATE_FORMAT);
    }
}
