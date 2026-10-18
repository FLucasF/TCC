package shop.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Arredondamento para centavos, "meio para o par". */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percent(BigDecimal base, BigDecimal percent) {
        return round(base.multiply(percent).movePointLeft(2));
    }
}
