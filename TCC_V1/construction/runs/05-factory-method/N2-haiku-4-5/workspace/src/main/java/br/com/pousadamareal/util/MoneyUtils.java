package br.com.pousadamareal.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    private static final int SCALE = 2;

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static String format(double value) {
        return String.format("%.2f", round(value));
    }

    public static String formatBRL(double value) {
        return String.format("R$ %,.2f", round(value)).replace(",", ".");
    }

    public static String formatBRLEnglish(double value) {
        double rounded = round(value);
        return String.format("BRL %.2f", rounded).replace(",", ".");
    }
}
