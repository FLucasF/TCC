package pousada.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Idioma, dinheiro e datas de um documento. */
public enum Language {
    PT_BR("R$ ", ',', '.', DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            "nº", "Hóspede", "Quarto", "Entrada", "Saída", "diária", "diárias", "Diárias", "Desconto", "Total"),
    EN("BRL ", '.', ',', DateTimeFormatter.ISO_LOCAL_DATE,
            "no.", "Guest", "Room", "Check-in", "Check-out", "night", "nights", "Nights", "Discount", "Total");

    private final String currency;
    private final char decimalSeparator;
    private final char groupingSeparator;
    private final DateTimeFormatter dateFormat;
    final String number;
    final String guest;
    final String room;
    final String checkIn;
    final String checkOut;
    final String nightOne;
    final String nightMany;
    final String subtotal;
    final String discount;
    final String total;

    Language(String currency, char decimalSeparator, char groupingSeparator, DateTimeFormatter dateFormat,
             String number, String guest, String room, String checkIn, String checkOut,
             String nightOne, String nightMany, String subtotal, String discount, String total) {
        this.currency = currency;
        this.decimalSeparator = decimalSeparator;
        this.groupingSeparator = groupingSeparator;
        this.dateFormat = dateFormat;
        this.number = number;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nightOne = nightOne;
        this.nightMany = nightMany;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }

    public String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(decimalSeparator);
        symbols.setGroupingSeparator(groupingSeparator);
        return currency + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public String date(LocalDate value) {
        return dateFormat.format(value);
    }

    String nights(int count) {
        return count + " " + (count == 1 ? nightOne : nightMany);
    }
}
