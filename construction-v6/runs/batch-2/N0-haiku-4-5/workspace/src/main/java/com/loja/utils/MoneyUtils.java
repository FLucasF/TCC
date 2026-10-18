package com.loja.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    public static double round(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
