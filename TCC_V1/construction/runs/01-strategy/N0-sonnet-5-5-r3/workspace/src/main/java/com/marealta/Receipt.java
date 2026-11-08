package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      String roomType, String checkIn, String checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r, Room room) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), r.roomNumber(), room.type().name(),
                r.checkIn().toString(), r.checkOut().toString(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Texto do comprovante (usado no corpo do e-mail). */
    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(LocalDate.parse(checkIn).format(BR_DATE))
                .append(" | Saída: ").append(LocalDate.parse(checkOut).format(BR_DATE))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        sb.append("Total: ").append(Money.format(total));
        return sb.toString();
    }
}
