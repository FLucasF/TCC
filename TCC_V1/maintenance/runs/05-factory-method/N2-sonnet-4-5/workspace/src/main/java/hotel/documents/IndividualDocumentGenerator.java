package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.time.LocalDate;

/** Generates the receipt for an individual guest (CPF). */
public final class IndividualDocumentGenerator implements DocumentGenerator {

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public String formatDocument(
            long reservationId, Guest guest, Room room, LocalDate checkIn, LocalDate checkOut, Price price) {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guest.name()).append(" (CPF ").append(guest.cpf()).append(")\n");
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? " diária" : " diárias")
                .append('\n');
        text.append("Diárias: ").append(TextFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(price.total())).append('\n');
        return text.toString();
    }
}
