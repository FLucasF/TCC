package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Booking receipt for a foreign guest with passport. */
public final class BookingReceipt extends ReservationDocument {

    private static final Locale US = Locale.US;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String guestPassport;
    private final String guestCountry;

    public BookingReceipt(Reservation reservation, Room room) {
        super(reservation, room);
        this.guestPassport = reservation.guest().passport();
        this.guestCountry = reservation.guest().country();
    }

    public String guestPassport() {
        return guestPassport;
    }

    public String guestCountry() {
        return guestCountry;
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(formatDate(checkIn))
                .append(" | Check-out: ").append(formatDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(formatMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(formatMoney(discount)).append('\n');
        }
        text.append("Total: ").append(formatMoney(total)).append('\n');
        return text.toString();
    }

    private static String formatMoney(BigDecimal amount) {
        return "BRL " + String.format(US, "%,.2f", amount);
    }

    private static String formatDate(LocalDate date) {
        return ISO_DATE.format(date);
    }
}
