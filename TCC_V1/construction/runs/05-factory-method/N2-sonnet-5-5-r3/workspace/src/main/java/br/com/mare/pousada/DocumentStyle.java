package br.com.mare.pousada;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Idioma e formatos de um documento: textos fixos, dinheiro e datas. */
public record DocumentStyle(String currency, DecimalFormatSymbols symbols, DateTimeFormatter dateFormat,
                            String numberSign, String roomLabel, String checkInLabel, String checkOutLabel,
                            String oneNight, String manyNights, String subtotalLabel, String discountLabel,
                            String totalLabel) {

    public static final DocumentStyle PORTUGUESE = new DocumentStyle("R$ ", symbols(',', '.'),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), "nº", "Quarto", "Entrada", "Saída",
            "diária", "diárias", "Diárias", "Desconto", "Total");

    public static final DocumentStyle ENGLISH = new DocumentStyle("BRL ", symbols('.', ','),
            DateTimeFormatter.ISO_LOCAL_DATE, "no.", "Room", "Check-in", "Check-out",
            "night", "nights", "Nights", "Discount", "Total");

    private static DecimalFormatSymbols symbols(char decimal, char grouping) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setDecimalSeparator(decimal);
        s.setGroupingSeparator(grouping);
        return s;
    }

    public String money(BigDecimal value) {
        return currency + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public String date(LocalDate date) {
        return dateFormat.format(date);
    }

    public String nights(int count) {
        return count + " " + (count == 1 ? oneNight : manyNights);
    }
}
