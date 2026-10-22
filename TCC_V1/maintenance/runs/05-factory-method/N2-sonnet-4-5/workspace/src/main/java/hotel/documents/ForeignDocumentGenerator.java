package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.time.LocalDate;

/** Generates the booking receipt for a foreign guest (passport). */
public final class ForeignDocumentGenerator implements DocumentGenerator {

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    public String formatDocument(
            long reservationId, Guest guest, Room room, LocalDate checkIn, LocalDate checkOut, Price price) {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ")
                .append(guest.name())
                .append(" (passport ")
                .append(guest.passport())
                .append(", ")
                .append(guest.country())
                .append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Check-in: ").append(EnglishFormat.date(checkIn))
                .append(" | Check-out: ").append(EnglishFormat.date(checkOut))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? " night" : " nights")
                .append('\n');
        text.append("Nights: ").append(EnglishFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Discount: ").append(EnglishFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(EnglishFormat.money(price.total())).append('\n');
        return text.toString();
    }
}
