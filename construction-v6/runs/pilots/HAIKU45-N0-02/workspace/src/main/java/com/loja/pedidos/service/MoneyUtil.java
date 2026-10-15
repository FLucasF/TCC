package com.loja.pedidos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static BigDecimal roundToNearestEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return roundToNearestEven(base.multiply(percentage).divide(HUNDRED, 10, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal divideRounded(BigDecimal dividend, BigDecimal divisor) {
        return roundToNearestEven(dividend.divide(divisor, 10, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal add(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return roundToNearestEven(sum);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return roundToNearestEven(a.subtract(b));
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
