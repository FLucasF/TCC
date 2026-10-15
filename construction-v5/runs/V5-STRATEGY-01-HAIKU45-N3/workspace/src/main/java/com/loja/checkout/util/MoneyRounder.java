package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    private static final int SCALE = 2;
    private static final RoundingMode MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(SCALE, MODE);
    }
}
