package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, RoomType roomType,
                      LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public String text() {
        StringBuilder text = new StringBuilder()
                .append(TITLE).append(" nº ").append(reservationId).append('\n')
                .append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n")
                .append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n")
                .append("Entrada: ").append(Money.brDate(checkIn)).append(" | Saída: ").append(Money.brDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        return text.append("Total: ").append(Money.brl(total)).toString();
    }
}
