package br.com.marealta.domain;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Booking receipt, o comprovante em inglês para hóspede estrangeiro. */
final class BookingReceiptDocument extends ReservationDocument {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String passport;
    private final String country;

    BookingReceiptDocument(Reservation reservation, String passport, String country) {
        super(reservation);
        this.passport = passport;
        this.country = country;
    }

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    protected String heading() {
        return title() + " no. " + reservation.id();
    }

    @Override
    protected List<String> guestLines() {
        return List.of("Guest: " + reservation.guestName() + " (passport " + passport + ", " + country + ")");
    }

    @Override
    protected String roomLine() {
        return "Room: " + reservation.room().number() + " (" + reservation.room().type() + ")";
    }

    @Override
    protected String periodLine() {
        int nights = reservation.nights();
        return "Check-in: " + DATE.format(reservation.checkIn()) + " | Check-out: "
                + DATE.format(reservation.checkOut()) + " | " + nights + (nights == 1 ? " night" : " nights");
    }

    @Override
    protected String subtotalLine() {
        return "Nights: " + Money.international(reservation.subtotal());
    }

    @Override
    protected String discountLine() {
        return "Discount: " + Money.international(reservation.discount());
    }

    @Override
    protected String totalLine() {
        return "Total: " + Money.international(reservation.total());
    }
}
