package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        BigDecimal total) {

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

    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservationId).append("\n");
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport).append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(isoDate(checkIn))
                .append(" | Check-out: ").append(isoDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append("\n");
        text.append("Nights: ").append(englishMoney(subtotal)).append("\n");
        if (discount.signum() > 0) {
            text.append("Discount: ").append(englishMoney(discount)).append("\n");
        }
        text.append("Total: ").append(englishMoney(total)).append("\n");
        return text.toString();
    }

    private static String isoDate(LocalDate date) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    private static String englishMoney(BigDecimal amount) {
        return String.format("BRL %,.2f", amount);
    }
}
