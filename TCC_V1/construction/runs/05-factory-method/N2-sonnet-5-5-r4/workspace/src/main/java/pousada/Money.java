package pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

final class Money {
    private Money() {
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal percent(BigDecimal value, String rate) {
        return round(value.multiply(new BigDecimal(rate)));
    }

    static String brl(BigDecimal value) {
        return "R$ " + String.format(Locale.GERMANY, "%,.2f", value);
    }

    static String brlEn(BigDecimal value) {
        return "BRL " + String.format(Locale.US, "%,.2f", value);
    }
}
