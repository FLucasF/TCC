package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

/** The words, and the way of writing money and dates, of the language a document is written in. */
record Wording(
        String numberSign,
        String guest,
        String room,
        String checkIn,
        String checkOut,
        String night,
        String nights,
        String subtotal,
        String discount,
        String total,
        Function<BigDecimal, String> money,
        Function<LocalDate, String> date) {

    static final Wording PORTUGUESE = new Wording(
            "nº", "Hóspede", "Quarto", "Entrada", "Saída", "diária", "diárias", "Diárias", "Desconto", "Total",
            TextFormat::money, TextFormat::date);

    static final Wording ENGLISH = new Wording(
            "no.", "Guest", "Room", "Check-in", "Check-out", "night", "nights", "Nights", "Discount", "Total",
            TextFormat::englishMoney, TextFormat::isoDate);
}
