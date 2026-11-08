package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return round(a.divide(b, 10, RoundingMode.HALF_EVEN));
    }
}
