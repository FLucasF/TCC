package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Generates the booking receipt for foreign guests in English. */
public final class ForeignerReceiptGenerator implements DocumentGenerator {

    @Override
    public ReservationReceipt generate(Reservation reservation, Room room) {
        String text = buildText(reservation, room);
        return new ReservationReceipt(
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
                reservation.price().total(),
                text);
    }

    private String buildText(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.id()).append('\n');
        text.append("Guest: ")
                .append(reservation.guest().name())
                .append(" (passport ")
                .append(reservation.guest().passport())
                .append(", ")
                .append(reservation.guest().country())
                .append(")\n");
        text.append("Room: ")
                .append(room.number())
                .append(" (")
                .append(room.type())
                .append(")\n");
        text.append("Check-in: ")
                .append(TextFormat.dateIso(reservation.checkIn()))
                .append(" | Check-out: ")
                .append(TextFormat.dateIso(reservation.checkOut()))
                .append(" | ")
                .append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " night" : " nights")
                .append('\n');
        text.append("Nights: ")
                .append(TextFormat.moneyEnglish(reservation.price().subtotal()))
                .append('\n');
        BigDecimal discount = reservation.price().discount();
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyEnglish(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyEnglish(reservation.price().total())).append('\n');
        return text.toString();
    }
}
