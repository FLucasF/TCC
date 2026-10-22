package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** English: BRL 1,234.56 and 2027-03-10. */
final class EnglishWording implements Wording {

    @Override
    public String money(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }

    @Override
    public String date(LocalDate date) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    @Override
    public String numberSign() {
        return "no.";
    }

    @Override
    public String guest() {
        return "Guest";
    }

    @Override
    public String room() {
        return "Room";
    }

    @Override
    public String checkIn() {
        return "Check-in";
    }

    @Override
    public String checkOut() {
        return "Check-out";
    }

    @Override
    public String nights(int nights) {
        return nights + (nights == 1 ? " night" : " nights");
    }

    @Override
    public String subtotal() {
        return "Nights";
    }

    @Override
    public String discount() {
        return "Discount";
    }

    @Override
    public String total() {
        return "Total";
    }
}
