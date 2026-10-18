package com.loja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounding {
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
