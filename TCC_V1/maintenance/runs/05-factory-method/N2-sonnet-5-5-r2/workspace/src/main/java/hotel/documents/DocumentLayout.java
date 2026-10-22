package hotel.documents;

import hotel.reservations.IdentificationFields;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/**
 * How a kind of document is written. The sequence of the text is fixed here; each kind of document gives only the
 * lines that are its own.
 */
public abstract class DocumentLayout {

    public final ReservationReceipt issue(Reservation reservation, Room room) {
        IdentificationFields id = reservation.guest().identification().fields();
        Price price = reservation.price();
        return new ReservationReceipt(
                title(),
                reservation.id(),
                reservation.guest().name(),
                id.cpf(),
                id.companyCnpj(),
                id.companyName(),
                id.guestPassport(),
                id.guestCountry(),
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

    private String text(Reservation reservation, Room room) {
        Price price = reservation.price();
        return headerLine(reservation.id())
                + identityLines(reservation)
                + roomLine(room)
                + stayLine(reservation)
                + priceLines(price)
                + closingLines(price);
    }

    protected abstract String title();

    protected abstract String headerLine(long reservationId);

    /** The lines that say who the document is for; each ends with a line break. */
    protected abstract String identityLines(Reservation reservation);

    protected abstract String roomLine(Room room);

    protected abstract String stayLine(Reservation reservation);

    /** The subtotal, the discount (only when there is one) and the total; each line ends with a line break. */
    protected abstract String priceLines(Price price);

    /** What comes after the total; nothing, unless the kind of document says otherwise. */
    protected String closingLines(Price price) {
        return "";
    }
}
