package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal round(double value) {
        return round(BigDecimal.valueOf(value));
    }
}
