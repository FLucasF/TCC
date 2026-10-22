package hotel.documents;

import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/**
 * The document the guest gets. Every document follows the same sequence of lines; each layout writes its own
 * lines (language, identification, extras at the end).
 */
public abstract class DocumentLayout {

    public abstract String title();

    protected abstract String header(Reservation reservation);

    protected abstract String guestLines(Reservation reservation);

    protected abstract String roomLine(Room room);

    protected abstract String stayLine(Reservation reservation);

    protected abstract String chargesLine(Price price);

    protected abstract String discountLine(Price price);

    protected abstract String totalLine(Price price);

    /** Lines after the total; none by default. */
    protected String closingLines(Price price) {
        return null;
    }

    public final String render(Reservation reservation, Room room) {
        Price price = reservation.price();
        StringBuilder text = new StringBuilder();
        text.append(header(reservation)).append('\n');
        text.append(guestLines(reservation)).append('\n');
        text.append(roomLine(room)).append('\n');
        text.append(stayLine(reservation)).append('\n');
        text.append(chargesLine(price)).append('\n');
        if (price.discount().signum() > 0) {
            text.append(discountLine(price)).append('\n');
        }
        text.append(totalLine(price)).append('\n');
        String closing = closingLines(price);
        if (closing != null) {
            text.append(closing).append('\n');
        }
        return text.toString();
    }
}
