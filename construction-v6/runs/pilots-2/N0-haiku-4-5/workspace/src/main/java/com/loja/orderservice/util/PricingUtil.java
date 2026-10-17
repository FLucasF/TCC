package com.loja.orderservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingUtil {
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return round(a.divide(b, SCALE, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal percentage(BigDecimal base, BigDecimal percent) {
        return round(base.multiply(percent).divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_EVEN));
    }

    public static int ceilDays(double hours) {
        return (int) Math.ceil(hours / 24.0);
    }
}
