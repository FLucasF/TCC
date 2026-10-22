package br.com.maralta.pousada;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Rótulos e formatos de um idioma de documento. */
public record Language(
        String numberLabel,
        String guest,
        String room,
        String checkIn,
        String checkOut,
        String items,
        String discount,
        String total,
        String nightOne,
        String nightMany,
        String currency,
        char thousands,
        char decimal,
        DateTimeFormatter dateFormat) {

    public static final Language PT = new Language("nº", "Hóspede", "Quarto", "Entrada", "Saída", "Diárias",
            "Desconto", "Total", "diária", "diárias", "R$ ", '.', ',', DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    public static final Language EN = new Language("no.", "Guest", "Room", "Check-in", "Check-out", "Nights",
            "Discount", "Total", "night", "nights", "BRL ", ',', '.', DateTimeFormatter.ISO_LOCAL_DATE);

    public String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(thousands);
        symbols.setDecimalSeparator(decimal);
        return currency + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public String date(LocalDate value) {
        return dateFormat.format(value);
    }

    public String nights(int count) {
        return count + " " + (count == 1 ? nightOne : nightMany);
    }
}
