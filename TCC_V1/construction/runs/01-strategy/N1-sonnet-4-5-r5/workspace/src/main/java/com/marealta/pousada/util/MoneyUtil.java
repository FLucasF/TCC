package com.marealta.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentage(BigDecimal value, BigDecimal percentageRate) {
        return round(value.multiply(percentageRate).divide(new BigDecimal("100"), 10, RoundingMode.HALF_EVEN));
    }
}
