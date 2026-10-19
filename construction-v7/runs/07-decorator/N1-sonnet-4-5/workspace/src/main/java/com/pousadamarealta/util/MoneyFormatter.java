package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormatter {
    private static final Locale BRAZIL = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(BRAZIL);

    public static String format(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}
