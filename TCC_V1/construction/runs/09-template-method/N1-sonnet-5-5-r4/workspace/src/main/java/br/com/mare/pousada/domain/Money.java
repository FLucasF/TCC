package br.com.mare.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Todo valor em dinheiro: duas casas, arredondamento meio para o par. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** 599.76, como vai nos CSV. */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }

    /** R$ 1.234,56, como vai nos comprovantes. */
    public static String display(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.of("pt", "BR")));
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(round(value));
    }
}
