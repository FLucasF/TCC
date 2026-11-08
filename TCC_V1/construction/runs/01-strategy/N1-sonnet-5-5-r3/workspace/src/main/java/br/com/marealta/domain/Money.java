package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dinheiro: sempre com 2 casas, arredondado "meio para o par" a cada passo. */
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

    /** {@code base * rate}, arredondado. Ex.: percent(779.80, "0.12") = 93.58. */
    public static BigDecimal percent(BigDecimal base, String rate) {
        return round(base.multiply(new BigDecimal(rate)));
    }

    /** Formato dos comprovantes: R$ 1.234,56. */
    public static String format(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        return "R$ " + format.format(value);
    }
}
