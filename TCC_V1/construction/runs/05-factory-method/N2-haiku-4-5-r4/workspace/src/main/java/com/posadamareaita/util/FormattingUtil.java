package com.posadamareaita.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormattingUtil {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatMoneyPT(BigDecimal value) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
        df.setRoundingMode(java.math.RoundingMode.HALF_EVEN);
        df.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(new java.util.Locale("pt", "BR")));
        return "R$ " + df.format(value);
    }

    public static String formatMoneyEN(BigDecimal value) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
        df.setRoundingMode(java.math.RoundingMode.HALF_EVEN);
        return "BRL " + df.format(value);
    }

    public static String formatDatePT(LocalDate date) {
        return date.format(PT_DATE_FORMAT);
    }

    public static String formatDateISO(LocalDate date) {
        return date.format(ISO_DATE_FORMAT);
    }

    public static String formatDateEN(LocalDate date) {
        return date.format(EN_DATE_FORMAT);
    }

    public static String formatNightsPT(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    public static String formatNightsEN(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }
}
