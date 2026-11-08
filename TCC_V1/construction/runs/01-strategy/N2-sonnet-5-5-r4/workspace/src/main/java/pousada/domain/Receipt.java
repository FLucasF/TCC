package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(
        String title, long reservationId, String guestName, String guestCpf, int roomNumber, RoomType roomType,
        LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, Room room) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), room.number(), room.type(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(checkIn.format(DATE)).append(" | Saída: ").append(checkOut.format(DATE))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        text.append("Total: ").append(Money.format(total));
        return text.toString();
    }
}
