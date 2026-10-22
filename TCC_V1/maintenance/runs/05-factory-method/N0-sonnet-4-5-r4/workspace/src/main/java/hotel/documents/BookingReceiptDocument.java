package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Booking receipt for foreign guests (in English). */
public record BookingReceiptDocument(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
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

    public BookingReceiptDocument(Reservation reservation, Room room) {
        this(
                "Booking receipt",
                reservation.id(),
                reservation.guest().name(),
                null,
                null,
                null,
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
        text.append("Check-in: ").append(TextFormat.isoDate(checkIn))
                .append(" | Check-out: ").append(TextFormat.isoDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyEnglish(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyEnglish(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyEnglish(total)).append('\n');
        return text.toString();
    }
}
