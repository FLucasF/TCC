package hotel.documents;

import hotel.reservations.ForeignGuest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Receipt for foreign guests (identified by passport). */
public record BookingReceipt(
        String title,
        long reservationId,
        String guestName,
        String passportNumber,
        String country,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) implements ReservationDocument {

    public BookingReceipt(Reservation reservation, Room room, ForeignGuest guest) {
        this(
                "Booking receipt",
                reservation.id(),
                guest.name(),
                guest.passportNumber(),
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
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(passportNumber)
                .append(", ").append(country).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(EnglishTextFormat.date(checkIn))
                .append(" | Check-out: ").append(EnglishTextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(EnglishTextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(EnglishTextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(EnglishTextFormat.money(total)).append('\n');
        return text.toString();
    }
}
