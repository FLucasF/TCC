package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.util.List;

/**
 * One kind of document the guest can get. The steps of the text are written once, here; each kind says what
 * is its own: the title, the language, who is identified, and what comes after the total.
 */
abstract class DocumentLayout {

    /** The kind of document that goes with the way the guest is identified. */
    static DocumentLayout forIdentification(Identification identification) {
        return switch (identification) {
            case Identification.Cpf cpf -> new ReceiptLayout();
            case Identification.Company company -> new InvoiceLayout();
            case Identification.Passport passport -> new BookingReceiptLayout();
        };
    }

    abstract String title();

    abstract Language language();

    /** The lines that say who the guest is, right after the title line. */
    abstract List<String> identityLines(Reservation reservation);

    /** The lines after the total. */
    List<String> closingLines(Reservation reservation) {
        return List.of();
    }

    final String text(Reservation reservation, Room room) {
        Language language = language();
        var price = reservation.price();
        int nights = price.nights();
        StringBuilder text = new StringBuilder();
        text.append(title()).append(' ').append(language.number).append(' ').append(reservation.id()).append('\n');
        identityLines(reservation).forEach(line -> text.append(line).append('\n'));
        text.append(language.room).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append(language.checkIn).append(": ").append(language.date(reservation.checkIn()))
                .append(" | ").append(language.checkOut).append(": ").append(language.date(reservation.checkOut()))
                .append(" | ").append(nights).append(' ')
                .append(nights == 1 ? language.nightSingular : language.nightPlural).append('\n');
        text.append(language.nights).append(": ").append(language.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append(language.discount).append(": ").append(language.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(language.money(price.total())).append('\n');
        closingLines(reservation).forEach(line -> text.append(line).append('\n'));
        return text.toString();
    }
}
