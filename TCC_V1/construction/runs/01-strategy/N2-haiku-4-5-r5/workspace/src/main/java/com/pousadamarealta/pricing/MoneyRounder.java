package com.pousadamarealta.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
