package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal percent(BigDecimal amount, String rate) {
        return round(amount.multiply(new BigDecimal(rate)));
    }
}
