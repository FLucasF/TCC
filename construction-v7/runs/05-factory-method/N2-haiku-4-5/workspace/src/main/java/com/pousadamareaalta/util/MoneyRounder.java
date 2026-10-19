package com.pousadamareaalta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }
}
