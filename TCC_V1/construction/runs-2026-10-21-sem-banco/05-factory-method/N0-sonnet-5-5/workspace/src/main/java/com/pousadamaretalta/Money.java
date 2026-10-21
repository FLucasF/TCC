package com.pousadamaretalta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Dinheiro: sempre 2 casas, arredondamento meio para o par. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** 1234.56 -> "R$ 1.234,56" */
    public static String brl(BigDecimal v) {
        String us = String.format(Locale.US, "%,.2f", round(v));
        return "R$ " + us.replace(',', '#').replace('.', ',').replace('#', '.');
    }

    /** 1234.56 -> "BRL 1,234.56" */
    public static String brlEn(BigDecimal v) {
        return "BRL " + String.format(Locale.US, "%,.2f", round(v));
    }
}
