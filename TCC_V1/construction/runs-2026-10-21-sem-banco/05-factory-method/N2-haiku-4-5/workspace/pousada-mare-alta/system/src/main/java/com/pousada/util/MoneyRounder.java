package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    public static double round(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
