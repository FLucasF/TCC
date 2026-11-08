package pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Fração de um valor (ex.: 0.12 de 779.80), já arredondada para centavos. */
    public static BigDecimal fraction(BigDecimal value, String fraction) {
        return round(value.multiply(new BigDecimal(fraction)));
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }
}
