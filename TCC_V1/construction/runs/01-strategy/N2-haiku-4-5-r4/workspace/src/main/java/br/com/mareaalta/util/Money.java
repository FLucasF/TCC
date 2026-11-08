package br.com.mareaalta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static String format(double value) {
        return String.format("%.2f", value);
    }

    public static String formatBr(double value) {
        String[] parts = String.format("%.2f", value).split("\\.");
        return "R$ " + parts[0].replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".") + "," + parts[1];
    }

    public static double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
