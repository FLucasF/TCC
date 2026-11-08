package pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    /** Centavos, arredondando meio para o par. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** 1234.56, como vai nos CSV. */
    public static String csv(BigDecimal value) {
        return round(value).toPlainString();
    }

    /** R$ 1.234,56, como vai nos comprovantes. */
    public static String brl(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(round(value));
    }
}
