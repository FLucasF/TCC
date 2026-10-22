package pousada;

import java.util.List;
import java.util.Map;

class BookingReceipt extends Document {
    @Override
    String title() {
        return "Booking receipt";
    }

    @Override
    protected String titleLine(Reservation r) {
        return title() + " no. " + r.id();
    }

    @Override
    protected List<String> identityLines(Reservation r) {
        Map<String, String> id = r.identification().values();
        return List.of("Guest: " + r.guestName() + " (passport " + id.get("guestPassport") + ", "
                + id.get("guestCountry") + ")");
    }

    @Override
    protected String roomLine(Reservation r) {
        return "Room: " + r.room().number() + " (" + r.room().type() + ")";
    }

    @Override
    protected String periodLine(Reservation r) {
        int nights = r.pricing().nights();
        return "Check-in: " + r.checkIn() + " | Check-out: " + r.checkOut() + " | "
                + nights + (nights == 1 ? " night" : " nights");
    }

    @Override
    protected String subtotalLine(Reservation r) {
        return "Nights: " + Money.brlEn(r.pricing().subtotal());
    }

    @Override
    protected String discountLine(Reservation r) {
        return "Discount: " + Money.brlEn(r.pricing().discount());
    }

    @Override
    protected String totalLine(Reservation r) {
        return "Total: " + Money.brlEn(r.pricing().total());
    }
}
