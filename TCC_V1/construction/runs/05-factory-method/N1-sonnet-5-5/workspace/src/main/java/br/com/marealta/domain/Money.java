package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
