package com.pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    public static double round(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static String format(double value) {
        return String.format("%.2f", value);
    }

    public static String formatBRL(double value) {
        return String.format("R$ %.2f", value).replace(".", ",");
    }
}
