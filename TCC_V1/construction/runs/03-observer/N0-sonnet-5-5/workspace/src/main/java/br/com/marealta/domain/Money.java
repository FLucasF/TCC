package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Arredondamento e formatação de dinheiro e datas. */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** R$ 1.234,56 */
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

    /** 10/03/2027 */
    public static String date(LocalDate d) {
        return BR_DATE.format(d);
    }
}
