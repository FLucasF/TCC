package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The receipt of a foreign guest: in English, with English money and ISO dates. */
public final class BookingReceiptLayout extends DocumentLayout {

    private static final Words WORDS = new Words(
            "no.", "Guest", "Room", "Check-in", "Check-out", "night", "nights", "Nights", "Discount", "Total");

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    protected Words words() {
        return WORDS;
    }

    @Override
    protected String money(BigDecimal amount) {
        return TextFormat.englishMoney(amount);
    }

    @Override
    protected String date(LocalDate date) {
        return TextFormat.isoDate(date);
    }

    @Override
    protected String guestLines(Reservation reservation) {
        Identification passport = reservation.guest().identification();
        return "Guest: " + reservation.guest().name() + " (passport " + passport.passport() + ", "
                + passport.country() + ")\n";
    }
}
