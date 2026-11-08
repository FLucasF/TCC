package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {
    public static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal roundHalfEven(double value) {
        return roundHalfEven(BigDecimal.valueOf(value));
    }
}
