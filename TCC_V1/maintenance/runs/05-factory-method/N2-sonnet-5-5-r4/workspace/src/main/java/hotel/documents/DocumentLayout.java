package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The text of a reservation document. The sequence of lines is the same for every kind of document and is written
 * here once; each kind says only what is its own: the language, the way money and dates are written, who is named
 * and what comes after the total.
 */
abstract class DocumentLayout {

    abstract String title();

    /** The word between the title and the reservation number ("nº", "no."). */
    abstract String numberMark();

    abstract String party(Guest guest);

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
    String closing(Price price) {
        return "";
    }

    final String text(Reservation reservation, Room room) {
        Price price = reservation.price();
        StringBuilder text = new StringBuilder();
        text.append(title()).append(' ').append(numberMark()).append(' ').append(reservation.id()).append('\n');
        text.append(party(reservation.guest()));
        text.append(roomLabel()).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append(checkInLabel()).append(": ").append(date(reservation.checkIn()))
                .append(" | ").append(checkOutLabel()).append(": ").append(date(reservation.checkOut()))
                .append(" | ").append(nights(price.nights())).append('\n');
        text.append(subtotalLabel()).append(": ").append(money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append(discountLabel()).append(": ").append(money(price.discount())).append('\n');
        }
        text.append(totalLabel()).append(": ").append(money(price.total())).append('\n');
        text.append(closing(price));
        return text.toString();
    }
}
