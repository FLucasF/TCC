package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {

    public static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal multiply(BigDecimal amount, BigDecimal multiplier) {
        return roundMoney(amount.multiply(multiplier));
    }

    public static BigDecimal percentage(BigDecimal amount, int percent) {
        BigDecimal factor = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN);
        return roundMoney(amount.multiply(factor));
    }
}
