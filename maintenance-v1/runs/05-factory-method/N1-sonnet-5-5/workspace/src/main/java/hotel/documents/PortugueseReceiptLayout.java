package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** The receipt in Portuguese, for guests identified by CPF. */
class PortugueseReceiptLayout implements DocumentLayout {

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public String text(Reservation reservation, Room room) {
        var price = reservation.price();
        int nights = price.nights();
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" nº ").append(reservation.id()).append('\n');
        text.append(whoLines(reservation));
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(price.total())).append('\n');
        text.append(closingLines(reservation));
        return text.toString();
    }

    /** The line(s) saying who the guest is. */
    String whoLines(Reservation reservation) {
        return "Hóspede: " + reservation.guest().name() + " (CPF " + reservation.guest().cpf() + ")\n";
    }

    /** What comes after the total, if anything. */
    String closingLines(Reservation reservation) {
        return "";
    }
}
