package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    public static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
