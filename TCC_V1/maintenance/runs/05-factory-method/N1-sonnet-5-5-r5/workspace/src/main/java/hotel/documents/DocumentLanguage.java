package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** The words, and the way money and dates are written, in the language a document is issued in. */
enum DocumentLanguage {

    PORTUGUESE("nº", "Hóspede", "Quarto", "Entrada", "Saída", " diária", " diárias", "Diárias", "Desconto", "Total",
            TextFormat::money, TextFormat::date),
    ENGLISH("no.", "Guest", "Room", "Check-in", "Check-out", " night", " nights", "Nights", "Discount", "Total",
            amount -> "BRL " + String.format(Locale.US, "%,.2f", amount), DateTimeFormatter.ISO_LOCAL_DATE::format);

    final String number;
    final String guest;
    final String room;
    final String checkIn;
    final String checkOut;
    final String night;
    final String nights;
    final String subtotal;
    final String discount;
    final String total;
    private final Function<BigDecimal, String> money;
    private final Function<LocalDate, String> date;

    DocumentLanguage(
            String number, String guest, String room, String checkIn, String checkOut, String night, String nights,
            String subtotal, String discount, String total, Function<BigDecimal, String> money,
            Function<LocalDate, String> date) {
        this.number = number;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.night = night;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.money = money;
        this.date = date;
    }

    String money(BigDecimal amount) {
        return money.apply(amount);
    }

    String date(LocalDate day) {
        return date.apply(day);
    }
}
