package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Generates the receipt for individual guests (CPF holders) in Portuguese. */
public final class IndividualReceiptGenerator implements DocumentGenerator {

    @Override
    public ReservationReceipt generate(Reservation reservation, Room room) {
        String text = buildText(reservation, room);
        return new ReservationReceipt(
                "Comprovante de reserva",
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                null,
                null,
                null,
                null,
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
        text.append("Comprovante de reserva nº ").append(reservation.id()).append('\n');
        text.append("Hóspede: ")
                .append(reservation.guest().name())
                .append(" (CPF ")
                .append(reservation.guest().cpf())
                .append(")\n");
        text.append("Quarto: ")
                .append(room.number())
                .append(" (")
                .append(room.type())
                .append(")\n");
        text.append("Entrada: ")
                .append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ")
                .append(TextFormat.date(reservation.checkOut()))
                .append(" | ")
                .append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " diária" : " diárias")
                .append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        BigDecimal discount = reservation.price().discount();
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        return text.toString();
    }
}
