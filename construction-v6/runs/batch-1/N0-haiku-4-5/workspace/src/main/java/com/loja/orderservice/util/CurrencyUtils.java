package com.loja.orderservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyUtils {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal roundToCents(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return roundToCents(a.multiply(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return roundToCents(a.divide(b, 10, ROUNDING_MODE));
    }

    public static BigDecimal add(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                result = result.add(value);
            }
        }
        return roundToCents(result);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return roundToCents(a.subtract(b));
    }
}
