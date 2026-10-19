package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {
    private static final int SCALE = 2;

    public static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }
}
