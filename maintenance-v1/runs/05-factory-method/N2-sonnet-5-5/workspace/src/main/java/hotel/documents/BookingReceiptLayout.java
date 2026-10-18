package hotel.documents;

/** The booking receipt for a guest with a passport, in English. */
final class BookingReceiptLayout implements DocumentLayout {

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    public String text(ReservationDocument document) {
        StringBuilder text = new StringBuilder();
        text.append(document.title()).append(" no. ").append(document.reservationId()).append('\n');
        text.append("Guest: ").append(document.guestName())
                .append(" (passport ").append(document.guestPassport())
                .append(", ").append(document.guestCountry()).append(")\n");
        text.append("Room: ").append(document.roomNumber()).append(" (").append(document.roomType()).append(")\n");
        text.append("Check-in: ").append(EnglishTextFormat.date(document.checkIn()))
                .append(" | Check-out: ").append(EnglishTextFormat.date(document.checkOut()))
                .append(" | ").append(document.nights()).append(document.nights() == 1 ? " night" : " nights")
                .append('\n');
        text.append("Nights: ").append(EnglishTextFormat.money(document.subtotal())).append('\n');
        if (document.discount().signum() > 0) {
            text.append("Discount: ").append(EnglishTextFormat.money(document.discount())).append('\n');
        }
        text.append("Total: ").append(EnglishTextFormat.money(document.total())).append('\n');
        return text.toString();
    }
}
