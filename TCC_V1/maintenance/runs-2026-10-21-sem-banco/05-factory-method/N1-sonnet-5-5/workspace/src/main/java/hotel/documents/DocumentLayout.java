package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** How one kind of document is written: the sequence of lines is the same, each kind words and formats it. */
abstract class DocumentLayout {

    abstract String title();

    abstract String numberSign();

    /** The line(s) that say who the document is for. */
    abstract List<String> identityLines(Reservation reservation);

    abstract String roomLabel();

    abstract String checkInLabel();

    abstract String checkOutLabel();

    abstract String nights(int count);

    abstract String subtotalLabel();

    abstract String discountLabel();

    abstract String totalLabel();

    abstract String money(BigDecimal amount);

    abstract String date(LocalDate date);

    /** Lines after the total; none by default. */
    List<String> closingLines(Reservation reservation) {
        return List.of();
    }

    final String render(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        line(text, title() + " " + numberSign() + " " + reservation.id());
        identityLines(reservation).forEach(identity -> line(text, identity));
        line(text, roomLabel() + ": " + room.number() + " (" + room.type() + ")");
        line(text, checkInLabel() + ": " + date(reservation.checkIn())
                + " | " + checkOutLabel() + ": " + date(reservation.checkOut())
                + " | " + nights(reservation.price().nights()));
        line(text, subtotalLabel() + ": " + money(reservation.price().subtotal()));
        if (reservation.price().discount().signum() > 0) {
            line(text, discountLabel() + ": " + money(reservation.price().discount()));
        }
        line(text, totalLabel() + ": " + money(reservation.price().total()));
        closingLines(reservation).forEach(closing -> line(text, closing));
        return text.toString();
    }

    private static void line(StringBuilder text, String line) {
        text.append(line).append('\n');
    }
}
