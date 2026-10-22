package hotel.documents;

import hotel.reservations.Guest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/** The receipt of a foreign guest, identified by passport, in English. */
final class BookingReceiptLayout extends DocumentLayout {

    @Override
    String title() {
        return "Booking receipt";
    }

    @Override
    String numberMark() {
        return "no.";
    }

    @Override
    String party(Guest guest) {
        return "Guest: " + guest.name() + " (passport " + guest.identification().passport() + ", "
                + guest.identification().country() + ")\n";
    }

    @Override
    String roomLabel() {
        return "Room";
    }

    @Override
    String checkInLabel() {
        return "Check-in";
    }

    @Override
    String checkOutLabel() {
        return "Check-out";
    }

    @Override
    String nights(int count) {
        return count + (count == 1 ? " night" : " nights");
    }

    @Override
    String subtotalLabel() {
        return "Nights";
    }

    @Override
    String discountLabel() {
        return "Discount";
    }

    @Override
    String totalLabel() {
        return "Total";
    }

    @Override
    String money(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }

    @Override
    String date(LocalDate date) {
        return date.toString();
    }
}
