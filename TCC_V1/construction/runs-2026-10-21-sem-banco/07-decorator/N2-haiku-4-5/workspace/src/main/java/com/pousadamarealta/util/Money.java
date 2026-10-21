package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal value, BigDecimal factor) {
        return round(value.multiply(factor));
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal percent(BigDecimal value, int percent) {
        BigDecimal factor = new BigDecimal(percent).divide(new BigDecimal(100), 10, RoundingMode.HALF_EVEN);
        return multiply(value, factor);
    }
}
