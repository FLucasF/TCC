package pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    /** Arredonda para centavos, "meio para o par". */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Formato do comprovante: R$ 1.234,56. */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    /** Formato do CSV: 599.76. */
    public static String plain(BigDecimal value) {
        return value.toPlainString();
    }
}
