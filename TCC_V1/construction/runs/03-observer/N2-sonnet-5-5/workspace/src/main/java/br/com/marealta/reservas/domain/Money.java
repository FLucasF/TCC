package br.com.marealta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Arredondamento e formatação de dinheiro e datas. */
public final class Money {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(value);
    }

    /** 10/03/2027 */
    public static String brDate(LocalDate date) {
        return BR_DATE.format(date);
    }
}
