package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Receipt for an individual guest with CPF. */
public final class IndividualReceipt extends ReservationDocument {

    private final String guestCpf;

    public IndividualReceipt(Reservation reservation, Room room) {
        super(reservation, room);
        this.guestCpf = reservation.guest().cpf();
    }

    public String guestCpf() {
        return guestCpf;
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        return text.toString();
    }
}
