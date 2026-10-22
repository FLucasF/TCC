package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/**
 * The document a guest gets. The sequence of lines is written here, once; each kind of document only says its
 * title, its language, how it names the guest and what comes after the total.
 */
public abstract class DocumentLayout {

    private final String title;
    private final Wording wording;

    DocumentLayout(String title, Wording wording) {
        this.title = title;
        this.wording = wording;
    }

    public final String title() {
        return title;
    }

    public final String text(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append(title).append(' ').append(wording.numberSign()).append(' ').append(reservation.id()).append('\n');
        text.append(identification(reservation.guest()));
        text.append(wording.room()).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        int nights = reservation.price().nights();
        text.append(wording.checkIn()).append(": ").append(wording.date().apply(reservation.checkIn()))
                .append(" | ").append(wording.checkOut()).append(": ")
                .append(wording.date().apply(reservation.checkOut()))
                .append(" | ").append(nights).append(' ')
                .append(nights == 1 ? wording.night() : wording.nights()).append('\n');
        text.append(wording.subtotal()).append(": ").append(wording.money().apply(reservation.price().subtotal()))
                .append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append(wording.discount()).append(": ").append(wording.money().apply(reservation.price().discount()))
                    .append('\n');
        }
        text.append(wording.total()).append(": ").append(wording.money().apply(reservation.price().total()))
                .append('\n');
        text.append(afterTotal(reservation));
        return text.toString();
    }

    /** The lines that say who the guest is, each ending in a line break. */
    abstract String identification(Guest guest);

    /** What the document adds after the total; nothing by default. */
    String afterTotal(Reservation reservation) {
        return "";
    }

    final Wording wording() {
        return wording;
    }
}
