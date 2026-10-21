package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The English booking receipt for a foreign guest. */
class BookingReceiptLayout extends DocumentLayout {

    @Override
    String title() {
        return "Booking receipt";
    }

    @Override
    String numberSign() {
        return "no.";
    }

    @Override
    List<String> identityLines(Reservation reservation) {
        Identification identification = reservation.guest().identification();
        return List.of("Guest: " + reservation.guest().name()
                + " (passport " + identification.passport() + ", " + identification.country() + ")");
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
        return TextFormat.englishMoney(amount);
    }

    @Override
    String date(LocalDate date) {
        return date.toString();
    }
}
