package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingReceipt(
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
        BigDecimal total)
        implements Document {

    public BookingReceipt(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().guestPassport(),
                reservation.guest().guestCountry(),
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
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(TextFormat.dateEnglish(checkIn))
                .append(" | Check-out: ").append(TextFormat.dateEnglish(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyEnglish(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyEnglish(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyEnglish(total)).append('\n');
        return text.toString();
    }
}
