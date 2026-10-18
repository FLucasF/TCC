package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    public static Double round(Double value) {
        if (value == null) return 0.0;
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static Double roundToPrecision(Double value, int scale) {
        if (value == null) return 0.0;
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
    }
}
