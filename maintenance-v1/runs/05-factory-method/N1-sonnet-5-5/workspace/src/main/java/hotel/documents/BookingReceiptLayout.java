package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/** The receipt in English, for foreign guests. */
class BookingReceiptLayout implements DocumentLayout {

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    public String text(Reservation reservation, Room room) {
        var price = reservation.price();
        Guest guest = reservation.guest();
        int nights = price.nights();
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" no. ").append(reservation.id()).append('\n');
        text.append("Guest: ").append(guest.name())
                .append(" (passport ").append(guest.passport()).append(", ").append(guest.country()).append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Check-in: ").append(date(reservation.checkIn()))
                .append(" | Check-out: ").append(date(reservation.checkOut()))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Discount: ").append(money(price.discount())).append('\n');
        }
        text.append("Total: ").append(money(price.total())).append('\n');
        return text.toString();
    }

    private static String money(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }

    private static String date(LocalDate date) {
        return date.toString();
    }
}
