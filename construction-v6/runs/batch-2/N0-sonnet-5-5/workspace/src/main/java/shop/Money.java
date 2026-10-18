package shop;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro tem 2 casas e usa arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percent(BigDecimal base, String rate) {
        return round(base.multiply(new BigDecimal(rate)));
    }
}
