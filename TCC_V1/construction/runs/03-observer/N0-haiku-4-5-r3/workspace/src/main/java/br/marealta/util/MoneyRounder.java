package br.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
