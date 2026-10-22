package br.com.marealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, RoomType roomType,
                      LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r, Room room) {
        Quote q = r.quote();
        return new Receipt(r.id(), r.guestName(), r.guestCpf(), room.number(), room.type(), r.checkIn(),
                r.checkOut(), q.nights(), q.subtotal(), q.discount(), q.total());
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(TITLE).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(Money.brDate(checkIn)).append(" | Saída: ")
                .append(Money.brDate(checkOut)).append(" | ").append(nights)
                .append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        text.append("Total: ").append(Money.brl(total));
        return text.toString();
    }
}
