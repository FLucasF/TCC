package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Arredondamento e formatação de valores e datas, no jeito da pousada. */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Ex.: R$ 1.234,56 */
    public static String formatar(BigDecimal valor) {
        return "R$ " + new DecimalFormat("#,##0.00", new DecimalFormatSymbols(PT_BR)).format(valor);
    }

    /** Ex.: 10/03/2027 */
    public static String formatar(LocalDate data) {
        return DATA.format(data);
    }
}
