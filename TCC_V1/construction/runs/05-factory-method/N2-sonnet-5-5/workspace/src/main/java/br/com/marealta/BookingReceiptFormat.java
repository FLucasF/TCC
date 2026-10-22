package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BookingReceiptFormat extends DocumentFormat {
    public String title() { return "Booking receipt"; }

    protected String numberMarker() { return "no."; }

    protected List<String> partyLines(Reservation r) {
        return List.of("Guest: " + r.guestName() + " (passport " + r.identity().get(PassportIdentity.PASSPORT)
                + ", " + r.identity().get(PassportIdentity.COUNTRY) + ")");
    }

    protected String roomLabel() { return "Room"; }

    protected String checkInLabel() { return "Check-in"; }

    protected String checkOutLabel() { return "Check-out"; }

    protected String nightsText(int nights) { return nights + (nights == 1 ? " night" : " nights"); }

    protected String subtotalLabel() { return "Nights"; }

    protected String discountLabel() { return "Discount"; }

    protected String totalLabel() { return "Total"; }

    protected String money(BigDecimal value) { return Money.english(value); }

    protected String date(LocalDate date) { return date.toString(); }
}
