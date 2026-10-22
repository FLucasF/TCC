package com.pousada.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormatterUtil {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatCurrency(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("R$ #,##0.00", symbols);
        return df.format(value);
    }

    public static String formatCurrencyCSV(BigDecimal value) {
        return String.format("%.2f", value);
    }

    public static String formatDatePT(LocalDate date) {
        return date.format(PT_DATE_FORMAT);
    }

    public static String formatDateISO(LocalDate date) {
        return date.format(ISO_DATE_FORMAT);
    }

    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, ISO_DATE_FORMAT);
    }

    public static String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }
}
