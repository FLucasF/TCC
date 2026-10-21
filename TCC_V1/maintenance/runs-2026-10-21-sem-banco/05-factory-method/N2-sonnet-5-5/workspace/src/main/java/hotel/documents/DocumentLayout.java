package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * How one kind of document is issued. The sequence of the text is written here once; each kind only
 * fills in the steps that are its own.
 */
public abstract class DocumentLayout {

    public final ReservationDocument issue(Reservation reservation, Room room) {
        Guest guest = reservation.guest();
        Price price = reservation.price();
        return new ReservationDocument(
                title(),
                reservation.id(),
                guest.name(),
                guest.identification().cpf(),
                guest.identification().companyCnpj(),
                guest.identification().companyName(),
                guest.identification().passport(),
                guest.identification().country(),
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
        StringBuilder text = new StringBuilder();
        text.append(headline(reservation.id())).append('\n');
        text.append(identification(reservation.guest()));
        text.append(roomLabel()).append(": ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append(stay(reservation.checkIn(), reservation.checkOut(), price.nights())).append('\n');
        text.append(subtotalLabel()).append(": ").append(money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append(discountLabel()).append(": ").append(money(price.discount())).append('\n');
        }
        text.append(totalLabel()).append(": ").append(money(price.total())).append('\n');
        text.append(closing(price.total()));
        return text.toString();
    }

    protected abstract String title();

    /** The first line, with the reservation number. */
    protected abstract String headline(long reservationId);

    /** The line(s) that say who the document is for, each ending in a line break. */
    protected abstract String identification(Guest guest);

    protected abstract String roomLabel();

    /** The line with the dates and the number of nights. */
    protected abstract String stay(LocalDate checkIn, LocalDate checkOut, int nights);

    protected abstract String subtotalLabel();

    protected abstract String discountLabel();

    protected abstract String totalLabel();

    protected abstract String money(BigDecimal amount);

    /** Lines after the total, each ending in a line break; none by default. */
    protected String closing(BigDecimal total) {
        return "";
    }
}
