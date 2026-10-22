package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    private static final int SCALE = 2;

    public static double round(double value) {
        return new BigDecimal(value)
            .setScale(SCALE, RoundingMode.HALF_EVEN)
            .doubleValue();
    }
}
