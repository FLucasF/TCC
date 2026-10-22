package br.com.marealta.plan;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {}

    /** Arredondamento para centavos, "meio para o par". */
    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    public static BigDecimal percentOf(BigDecimal amount, String fraction) {
        return round(amount.multiply(new BigDecimal(fraction)));
    }

    /** Formata como "R$ 1.234,56". */
    public static String brl(BigDecimal v) {
        String plain = round(v).abs().toPlainString();
        int dot = plain.indexOf('.');
        String ints = plain.substring(0, dot);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.length(); i++) {
            if (i > 0 && (ints.length() - i) % 3 == 0) sb.append('.');
            sb.append(ints.charAt(i));
        }
        return (v.signum() < 0 ? "-R$ " : "R$ ") + sb + "," + plain.substring(dot + 1);
    }
}
