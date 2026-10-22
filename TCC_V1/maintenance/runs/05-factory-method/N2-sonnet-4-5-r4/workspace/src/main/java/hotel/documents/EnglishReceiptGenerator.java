package hotel.documents;

import hotel.reservations.ForeignIdentity;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

final class EnglishReceiptGenerator implements DocumentGenerator {

    @Override
    public ReservationReceipt generate(Reservation reservation, Room room) {
        ForeignIdentity identity = (ForeignIdentity) reservation.guest().identity();
        String text = buildText(reservation, room, identity);
        return new ReservationReceipt(
                "Booking receipt",
                reservation.id(),
                reservation.guest().name(),
                null,
                null,
                null,
                identity.passport(),
                identity.country(),
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

    private String buildText(Reservation reservation, Room room, ForeignIdentity identity) {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.id()).append('\n');
        text.append("Guest: ").append(reservation.guest().name())
                .append(" (passport ").append(identity.passport())
                .append(", ").append(identity.country()).append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Check-in: ").append(TextFormat.dateEnglish(reservation.checkIn()))
                .append(" | Check-out: ").append(TextFormat.dateEnglish(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyEnglish(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyEnglish(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyEnglish(reservation.price().total())).append('\n');
        return text.toString();
    }
}
