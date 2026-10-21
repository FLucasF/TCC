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

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(TITLE).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(Formats.date(checkIn))
                .append(" | Saída: ").append(Formats.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Formats.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Formats.money(discount)).append('\n');
        }
        text.append("Total: ").append(Formats.money(total));
        return text.toString();
    }
}
