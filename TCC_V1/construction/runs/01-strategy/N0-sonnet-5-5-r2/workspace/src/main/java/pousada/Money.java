package pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Todo valor em dinheiro passa por aqui: centavos, arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** base * fator, arredondado (fator como 0.15 para 15%). */
    public static BigDecimal times(BigDecimal base, BigDecimal factor) {
        return round(base.multiply(factor));
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setDecimalSeparator(',');
        s.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", s).format(round(v));
    }

    /** 1234.56, para JSON e CSV. */
    public static String plain(BigDecimal v) {
        return round(v).toPlainString();
    }
}
