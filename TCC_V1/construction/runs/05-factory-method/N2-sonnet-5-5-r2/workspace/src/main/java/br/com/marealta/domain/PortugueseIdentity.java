package br.com.marealta.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Documentos em português: R$ 1.234,56 e 10/03/2027. */
abstract class PortugueseIdentity extends Identity {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected String numberMark() {
        return "nº";
    }

    @Override
    protected String roomLabel() {
        return "Quarto";
    }

    @Override
    protected String periodLine(ReceiptData d) {
        return "Entrada: " + d.checkIn().format(DATE) + " | Saída: " + d.checkOut().format(DATE)
                + " | " + d.nights() + (d.nights() == 1 ? " diária" : " diárias");
    }

    @Override
    protected String amountLabel() {
        return "Diárias";
    }

    @Override
    protected String discountLabel() {
        return "Desconto";
    }

    @Override
    protected String totalLabel() {
        return "Total";
    }

    @Override
    protected String money(BigDecimal value) {
        return "R$ " + format(value);
    }

    static String format(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(value);
    }
}
