package br.com.pousada.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyFormatter {

    private static final DecimalFormatSymbols PT_BR_SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "BR"));
    private static final DecimalFormat PT_BR_FORMAT = new DecimalFormat("#,##0.00", PT_BR_SYMBOLS);

    private static final DecimalFormatSymbols EN_SYMBOLS = new DecimalFormatSymbols(Locale.US);
    private static final DecimalFormat EN_FORMAT = new DecimalFormat("#,##0.00", EN_SYMBOLS);

    public static String formatPtBr(BigDecimal amount) {
        return "R$ " + PT_BR_FORMAT.format(amount);
    }

    public static String formatEn(BigDecimal amount) {
        return "BRL " + EN_FORMAT.format(amount);
    }

    public static String formatCsv(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("0.00", symbols);
        return format.format(amount);
    }
}
