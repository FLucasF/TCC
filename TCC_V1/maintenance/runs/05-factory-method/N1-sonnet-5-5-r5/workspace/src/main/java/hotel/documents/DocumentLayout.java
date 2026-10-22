package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * How one kind of document reads. The steps are the same for every kind and written here once; each kind
 * gives its title and language, who the document is for, and what it adds at the end.
 */
abstract class DocumentLayout {

    private final String title;
    private final DocumentLanguage language;

    DocumentLayout(String title, DocumentLanguage language) {
        this.title = title;
        this.language = language;
    }

    /** The layout of the document a guest identified this way gets. */
    static DocumentLayout forGuest(Identification identification) {
        return switch (identification) {
            case Identification.Cpf cpf -> new ReceiptLayout();
            case Identification.Company company -> new InvoiceLayout();
            case Identification.Passport passport -> new BookingReceiptLayout();
        };
    }

    String title() {
        return title;
    }

    /** The lines, each ending in a line break, that say who the document is for. */
    abstract String guestLines(Reservation reservation);

    /** The lines, each ending in a line break, that come after the total. */
    String closingLines(Reservation reservation) {
        return "";
    }

    final String money(BigDecimal amount) {
        return language.money(amount);
    }

    final String text(Reservation reservation, Room room) {
        Price price = reservation.price();
        StringBuilder text = new StringBuilder();
        text.append(title).append(' ').append(language.number).append(' ').append(reservation.id()).append('\n');
        text.append(guestLines(reservation));
        text.append(language.room).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append(language.checkIn).append(": ").append(language.date(reservation.checkIn()))
                .append(" | ").append(language.checkOut).append(": ").append(language.date(reservation.checkOut()))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? language.night : language.nights)
                .append('\n');
        text.append(language.subtotal).append(": ").append(language.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append(language.discount).append(": ").append(language.money(price.discount())).append('\n');
        }
        text.append(language.total).append(": ").append(language.money(price.total())).append('\n');
        text.append(closingLines(reservation));
        return text.toString();
    }

    final String guestName(Reservation reservation) {
        return language.guest + ": " + reservation.guest().name();
    }
}
