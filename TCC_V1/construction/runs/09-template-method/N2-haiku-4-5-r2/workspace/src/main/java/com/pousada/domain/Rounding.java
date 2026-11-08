package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Rounding {
    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
