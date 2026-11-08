package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String formatBRL(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    public static String formatCSV(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN).toString().replace('.', ',').replace(',', '.');
    }
}
