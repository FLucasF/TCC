package hotel.documents;

import hotel.reservations.Guest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/** The booking receipt of a foreign guest, written in English. */
public final class BookingReceiptLayout extends DocumentLayout {

    @Override
    protected String title() {
        return "Booking receipt";
    }

    @Override
    protected String headline(long reservationId) {
        return title() + " no. " + reservationId;
    }

    @Override
    protected String identification(Guest guest) {
        return "Guest: " + guest.name() + " (passport " + guest.identification().passport()
                + ", " + guest.identification().country() + ")\n";
    }

    @Override
    protected String roomLabel() {
        return "Room";
    }

    @Override
    protected String stay(LocalDate checkIn, LocalDate checkOut, int nights) {
        return "Check-in: " + checkIn + " | Check-out: " + checkOut
                + " | " + nights + (nights == 1 ? " night" : " nights");
    }

    @Override
    protected String subtotalLabel() {
        return "Nights";
    }

    @Override
    protected String discountLabel() {
        return "Discount";
    }

    @Override
    protected String totalLabel() {
        return "Total";
    }

    @Override
    protected String money(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }
}
