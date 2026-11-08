package pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Valores em dinheiro: sempre com 2 casas, arredondados meio para o par. */
public final class Money {
    public static final BigDecimal ZERO = of("0");

    private Money() {
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual (ex.: 12 para 12%) de um valor, arredondado. */
    public static BigDecimal percent(BigDecimal value, int percent) {
        return round(value.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100)));
    }

    /** Formato do comprovante: R$ 1.234,56. */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(round(value));
    }
}
