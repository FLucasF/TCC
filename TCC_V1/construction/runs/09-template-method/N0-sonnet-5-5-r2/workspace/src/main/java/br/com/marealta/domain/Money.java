package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dinheiro: sempre em centavos, arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Formato dos relatórios CSV e do JSON: 1234.56 */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }

    /** Formato dos textos para o hóspede: R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(round(value));
    }
}
