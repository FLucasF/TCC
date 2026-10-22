package pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro tem 2 casas, com arredondamento meio para o par a cada passo. */
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

    public static BigDecimal times(BigDecimal value, String factor) {
        return round(value.multiply(new BigDecimal(factor)));
    }
}
