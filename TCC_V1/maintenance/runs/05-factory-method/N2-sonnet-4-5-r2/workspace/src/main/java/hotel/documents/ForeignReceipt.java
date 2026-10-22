package hotel.documents;

import hotel.reservations.ForeignGuest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The booking receipt a foreign guest gets (passport identification, in English). */
public record ForeignReceipt(
        long reservationId,
        String guestName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) implements ReservationDocument {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Locale US = Locale.US;

    public ForeignReceipt(Reservation reservation, Room room, ForeignGuest guest) {
        this(
                reservation.id(),
                guest.name(),
                guest.passport(),
                guest.country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    @Override
    public String guestCpf() {
        return null;
    }

    @Override
    public String companyCnpj() {
        return null;
    }

    @Override
    public String companyName() {
        return null;
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
