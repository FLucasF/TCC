package br.com.mare.pousada;

import java.util.List;

public class BookingReceiptDocument extends Document {
    private final PassportIdentity guest;

    public BookingReceiptDocument(Reservation reservation, PassportIdentity guest) {
        super(reservation);
        this.guest = guest;
    }

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    protected DocumentStyle style() {
        return DocumentStyle.ENGLISH;
    }

    @Override
    protected List<String> guestLines() {
        return List.of("Guest: " + reservation.guestName()
                + " (passport " + guest.passport() + ", " + guest.country() + ")");
    }
}
