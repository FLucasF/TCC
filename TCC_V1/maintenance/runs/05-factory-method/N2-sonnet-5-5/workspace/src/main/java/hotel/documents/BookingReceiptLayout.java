package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** The booking receipt, in English, for the foreign guest. */
public class BookingReceiptLayout extends DocumentLayout {

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    protected String header(Reservation reservation) {
        return title() + " no. " + reservation.id();
    }

    @Override
    protected String guestLines(Reservation reservation) {
        Identification guest = reservation.guest().identification();
        return "Guest: " + reservation.guest().name() + " (passport " + guest.passport() + ", " + guest.country() + ")";
    }

    @Override
    protected String roomLine(Room room) {
        return "Room: " + room.number() + " (" + room.type() + ")";
    }

    @Override
    protected String stayLine(Reservation reservation) {
        int nights = reservation.price().nights();
        return "Check-in: " + TextFormat.englishDate(reservation.checkIn())
                + " | Check-out: " + TextFormat.englishDate(reservation.checkOut())
                + " | " + nights + (nights == 1 ? " night" : " nights");
    }

    @Override
    protected String chargesLine(Price price) {
        return "Nights: " + TextFormat.englishMoney(price.subtotal());
    }

    @Override
    protected String discountLine(Price price) {
        return "Discount: " + TextFormat.englishMoney(price.discount());
    }

    @Override
    protected String totalLine(Price price) {
        return "Total: " + TextFormat.englishMoney(price.total());
    }
}
