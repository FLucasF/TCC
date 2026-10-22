package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * The document a kind of guest gets. The sequence of lines is the same for every kind and is written here;
 * each kind gives its title, its words, how it writes money and dates, who the guest is and what comes last.
 */
abstract class DocumentLayout {

    /** The words of a language, and how it writes money and dates. */
    record Wording(
            String number,
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
    }

    static final Wording PORTUGUESE = new Wording(
            "nº", "Quarto", "Entrada", "Saída", "diária", "diárias", "Diárias", "Desconto", "Total",
            TextFormat::money, TextFormat::date);
    static final Wording ENGLISH = new Wording(
            "no.", "Room", "Check-in", "Check-out", "night", "nights", "Nights", "Discount", "Total",
            TextFormat::englishMoney, TextFormat::englishDate);

    /** The layout for the way the guest is identified. */
    static DocumentLayout of(Identification identification) {
        return switch (identification) {
            case Identification.PersonalId personal -> new ReceiptLayout();
            case Identification.Company company -> new InvoiceLayout();
            case Identification.Foreigner foreigner -> new BookingReceiptLayout();
        };
    }

    private final Wording wording;

    DocumentLayout(Wording wording) {
        this.wording = wording;
    }

    abstract String title();

    /** The line(s) that say who the guest is, each ending in a line break. */
    abstract String guestLines(Reservation reservation);

    /** What comes after the total; nothing by default. */
    String footer(Reservation reservation) {
        return "";
    }

    final String text(Reservation reservation, Room room) {
        var price = reservation.price();
        Function<BigDecimal, String> money = wording.money();
        Function<LocalDate, String> date = wording.date();
        StringBuilder text = new StringBuilder();
        text.append(title()).append(' ').append(wording.number()).append(' ').append(reservation.id()).append('\n');
        text.append(guestLines(reservation));
        text.append(wording.room()).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append(wording.checkIn()).append(": ").append(date.apply(reservation.checkIn()))
                .append(" | ").append(wording.checkOut()).append(": ").append(date.apply(reservation.checkOut()))
                .append(" | ").append(price.nights()).append(' ')
                .append(price.nights() == 1 ? wording.night() : wording.nights()).append('\n');
        text.append(wording.subtotal()).append(": ").append(money.apply(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append(wording.discount()).append(": ").append(money.apply(price.discount())).append('\n');
        }
        text.append(wording.total()).append(": ").append(money.apply(price.total())).append('\n');
        text.append(footer(reservation));
        return text.toString();
    }

    String money(BigDecimal amount) {
        return wording.money().apply(amount);
    }
}
