package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, ROUNDING_MODE);
        return bd.doubleValue();
    }

    public static double applyPercentageDiscount(double value, double discountPercent) {
        BigDecimal bd = new BigDecimal(value);
        BigDecimal discount = new BigDecimal(discountPercent);
        BigDecimal result = bd.multiply(discount.divide(new BigDecimal(100), 10, ROUNDING_MODE));
        return result.setScale(2, ROUNDING_MODE).doubleValue();
    }

    public static double subtract(double a, double b) {
        BigDecimal bda = new BigDecimal(a);
        BigDecimal bdb = new BigDecimal(b);
        return bda.subtract(bdb).setScale(2, ROUNDING_MODE).doubleValue();
    }

    public static double add(double a, double b) {
        BigDecimal bda = new BigDecimal(a);
        BigDecimal bdb = new BigDecimal(b);
        return bda.add(bdb).setScale(2, ROUNDING_MODE).doubleValue();
    }
}
