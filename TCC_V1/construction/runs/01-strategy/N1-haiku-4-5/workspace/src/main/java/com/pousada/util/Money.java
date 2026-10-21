package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    public static double round(double value) {
        return round(BigDecimal.valueOf(value)).doubleValue();
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String format(double value) {
        return String.format("%.2f", value);
    }

    public static String formatForCsv(double value) {
        return format(value);
    }

    public static String formatForReceipt(double value) {
        return "R$ " + formatBrazilian(value);
    }

    public static String formatBrazilian(double value) {
        String formatted = format(value);
        String[] parts = formatted.split("\\.");
        return parts[0].replaceAll("\\B(?=(\\d{3})+$)", ".") + "," + parts[1];
    }
}
