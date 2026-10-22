package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/** The words, and the way money and dates are written, in the language a document is issued in. */
enum Language {

    PORTUGUESE("nº", "Hóspede", "Quarto", "Entrada", "Saída", "diária", "diárias", "Diárias", "Desconto") {
        @Override
        String money(BigDecimal amount) {
            return TextFormat.money(amount);
        }

        @Override
        String date(LocalDate date) {
            return TextFormat.date(date);
        }
    },

    ENGLISH("no.", "Guest", "Room", "Check-in", "Check-out", "night", "nights", "Nights", "Discount") {
        @Override
        String money(BigDecimal amount) {
            return "BRL " + String.format(Locale.US, "%,.2f", amount);
        }

        @Override
        String date(LocalDate date) {
            return date.toString();
        }
    };

    final String number;
    final String guest;
    final String room;
    final String checkIn;
    final String checkOut;
    final String nightSingular;
    final String nightPlural;
    final String nights;
    final String discount;

    Language(String number, String guest, String room, String checkIn, String checkOut, String nightSingular,
            String nightPlural, String nights, String discount) {
        this.number = number;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nightSingular = nightSingular;
        this.nightPlural = nightPlural;
        this.nights = nights;
        this.discount = discount;
    }

    abstract String money(BigDecimal amount);

    abstract String date(LocalDate date);
}
