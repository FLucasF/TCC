package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
