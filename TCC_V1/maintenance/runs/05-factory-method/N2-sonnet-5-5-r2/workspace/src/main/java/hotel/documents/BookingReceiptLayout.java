package hotel.documents;

import hotel.reservations.IdentificationFields;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** The receipt for a foreign guest with a passport, in English. */
public final class BookingReceiptLayout extends DocumentLayout {

    @Override
    protected String title() {
        return "Booking receipt";
    }

    @Override
    protected String headerLine(long reservationId) {
        return title() + " no. " + reservationId + "\n";
    }

    @Override
    protected String identityLines(Reservation reservation) {
        IdentificationFields guest = reservation.guest().identification().fields();
        return "Guest: " + reservation.guest().name()
                + " (passport " + guest.guestPassport() + ", " + guest.guestCountry() + ")\n";
    }

    @Override
    protected String roomLine(Room room) {
        return "Room: " + room.number() + " (" + room.type() + ")\n";
    }

    @Override
    protected String stayLine(Reservation reservation) {
        int nights = reservation.price().nights();
        return "Check-in: " + TextFormat.englishDate(reservation.checkIn())
                + " | Check-out: " + TextFormat.englishDate(reservation.checkOut())
                + " | " + nights + (nights == 1 ? " night" : " nights") + "\n";
    }

    @Override
    protected String priceLines(Price price) {
        String lines = "Nights: " + TextFormat.englishMoney(price.subtotal()) + "\n";
        if (price.discount().signum() > 0) {
            lines += "Discount: " + TextFormat.englishMoney(price.discount()) + "\n";
        }
        return lines + "Total: " + TextFormat.englishMoney(price.total()) + "\n";
    }
}
