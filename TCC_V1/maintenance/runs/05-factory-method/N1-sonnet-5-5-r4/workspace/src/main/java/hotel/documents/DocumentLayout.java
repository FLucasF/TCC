package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * How a reservation document reads: the sequence of lines is written here once, and each kind of
 * document only gives its own words, formats and extra lines.
 */
public abstract class DocumentLayout {

    /** The words of a document's fixed lines. */
    protected record Words(
            String number, String guest, String room, String checkIn, String checkOut, String night,
            String nights, String subtotal, String discount, String total) {
    }

    public abstract String title();

    protected abstract Words words();

    protected abstract String money(BigDecimal amount);

    protected abstract String date(LocalDate date);

    /** The line (or lines, each ending in a line break) that says who the guest is. */
    protected abstract String guestLines(Reservation reservation);

    /** Lines after the total; none unless the document has them. */
    protected String closingLines(Reservation reservation) {
        return "";
    }

    public final String text(Reservation reservation, Room room) {
        Words words = words();
        int nights = reservation.price().nights();
        BigDecimal discount = reservation.price().discount();
        StringBuilder text = new StringBuilder();
        text.append(title()).append(' ').append(words.number()).append(' ').append(reservation.id()).append('\n');
        text.append(guestLines(reservation));
        text.append(words.room()).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append(words.checkIn()).append(": ").append(date(reservation.checkIn()))
                .append(" | ").append(words.checkOut()).append(": ").append(date(reservation.checkOut()))
                .append(" | ").append(nights).append(' ').append(nights == 1 ? words.night() : words.nights())
                .append('\n');
        text.append(words.subtotal()).append(": ").append(money(reservation.price().subtotal())).append('\n');
        if (discount.signum() > 0) {
            text.append(words.discount()).append(": ").append(money(discount)).append('\n');
        }
        text.append(words.total()).append(": ").append(money(reservation.price().total())).append('\n');
        text.append(closingLines(reservation));
        return text.toString();
    }
}
