package br.com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyUtil {
    private static final int SCALE = 2;

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static double applyPercentage(double value, double percentage) {
        BigDecimal bd = new BigDecimal(value);
        BigDecimal multiplier = new BigDecimal(1 + percentage / 100.0);
        return bd.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static double calculateDiscount(double subtotal, double discountPercentage) {
        BigDecimal bd = new BigDecimal(subtotal);
        BigDecimal percentage = new BigDecimal(discountPercentage / 100.0);
        return bd.multiply(percentage).setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static String formatBr(double value) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(value);
    }

    public static String formatJson(double value) {
        return String.format("%.2f", value);
    }
}
