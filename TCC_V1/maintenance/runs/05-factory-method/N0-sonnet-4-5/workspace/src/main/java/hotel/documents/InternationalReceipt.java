package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record InternationalReceipt(
        String title,
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
        BigDecimal total) {

    public InternationalReceipt(Reservation reservation, Room room) {
        this(
                "Booking receipt",
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().passport(),
                reservation.guest().country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(formatIsoDate(checkIn))
                .append(" | Check-out: ").append(formatIsoDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(formatInternationalMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(formatInternationalMoney(discount)).append('\n');
        }
        text.append("Total: ").append(formatInternationalMoney(total)).append('\n');
        return text.toString();
    }

    private static String formatIsoDate(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String formatInternationalMoney(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }
}
