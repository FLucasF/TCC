package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r, Room room) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), room.number(), room.type(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String toText() {
        StringBuilder text = new StringBuilder()
                .append(title).append(" nº ").append(reservationId).append('\n')
                .append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n")
                .append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n")
                .append("Entrada: ").append(Formats.date(checkIn))
                .append(" | Saída: ").append(Formats.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Formats.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Formats.money(discount)).append('\n');
        }
        return text.append("Total: ").append(Formats.money(total)).toString();
    }
}
