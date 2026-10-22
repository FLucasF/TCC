package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.util.ArrayList;
import java.util.List;

/**
 * The document a reservation gets. The sequence of lines is written once here; each kind of document says
 * only its title, its language, how the guest is identified and what it adds at the end.
 */
public abstract class DocumentLayout {

    private final Wording wording;

    DocumentLayout(Wording wording) {
        this.wording = wording;
    }

    public final ReservationReceipt issue(Reservation reservation, Room room) {
        Identification identification = reservation.guest().identification();
        Price price = reservation.price();
        return new ReservationReceipt(
                title(),
                reservation.id(),
                reservation.guest().name(),
                identification.cpf(),
                identification.companyCnpj(),
                identification.companyName(),
                identification.passport(),
                identification.country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                price.nights(),
                price.subtotal(),
                price.discount(),
                price.total(),
                text(reservation, room));
    }

    abstract String title();

    /** The lines that say who the document is for. */
    abstract List<String> partyLines(Reservation reservation);

    /** What comes after the total. */
    List<String> closingLines(Price price) {
        return List.of();
    }

    final String guestLine(Reservation reservation, String identity) {
        return wording.guest() + ": " + reservation.guest().name() + " (" + identity + ")";
    }

    final Wording wording() {
        return wording;
    }

    private String text(Reservation reservation, Room room) {
        Price price = reservation.price();
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + wording.numberSign() + " " + reservation.id());
        lines.addAll(partyLines(reservation));
        lines.add(wording.room() + ": " + room.number() + " (" + room.type() + ")");
        lines.add(wording.checkIn() + ": " + wording.date(reservation.checkIn())
                + " | " + wording.checkOut() + ": " + wording.date(reservation.checkOut())
                + " | " + wording.nights(price.nights()));
        lines.add(wording.subtotal() + ": " + wording.money(price.subtotal()));
        if (price.discount().signum() > 0) {
            lines.add(wording.discount() + ": " + wording.money(price.discount()));
        }
        lines.add(wording.total() + ": " + wording.money(price.total()));
        lines.addAll(closingLines(price));
        return String.join("\n", lines) + "\n";
    }
}
