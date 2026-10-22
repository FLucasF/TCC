package pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em dinheiro: sempre 2 casas, arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** "1.234,56" (pt-BR) */
    public static String br(BigDecimal v) {
        return format(v, '.', ',');
    }

    /** "1,234.56" (en) */
    public static String en(BigDecimal v) {
        return format(v, ',', '.');
    }

    /** "1234.56" (CSV) */
    public static String plain(BigDecimal v) {
        return round(v).toPlainString();
    }

    private static String format(BigDecimal v, char group, char decimal) {
        String plain = round(v).abs().toPlainString();
        int dot = plain.indexOf('.');
        String ints = plain.substring(0, dot);
        String frac = plain.substring(dot + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.length(); i++) {
            if (i > 0 && (ints.length() - i) % 3 == 0) sb.append(group);
            sb.append(ints.charAt(i));
        }
        return (v.signum() < 0 ? "-" : "") + sb + decimal + frac;
    }
}
