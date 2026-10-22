package br.com.marealta.identity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Rótulos e formatos de um idioma de documento. */
public record Language(String numberLabel, String guest, String room, String checkIn, String checkOut,
                       String nightSingular, String nightPlural, String subtotal, String discount, String total,
                       String currencyPrefix, char groupSeparator, char decimalSeparator, String datePattern) {

    public static final Language PT_BR = new Language("nº", "Hóspede", "Quarto", "Entrada", "Saída",
            "diária", "diárias", "Diárias", "Desconto", "Total", "R$ ", '.', ',', "dd/MM/yyyy");

    public static final Language EN = new Language("no.", "Guest", "Room", "Check-in", "Check-out",
            "night", "nights", "Nights", "Discount", "Total", "BRL ", ',', '.', "yyyy-MM-dd");

    public String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(groupSeparator);
        symbols.setDecimalSeparator(decimalSeparator);
        return currencyPrefix + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public String date(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(datePattern, Locale.ROOT));
    }

    public String nights(int count) {
        return count + " " + (count == 1 ? nightSingular : nightPlural);
    }
}
