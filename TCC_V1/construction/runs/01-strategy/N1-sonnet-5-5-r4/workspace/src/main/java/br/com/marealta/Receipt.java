package br.com.marealta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    private static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), r.roomNumber(), r.room().type().name(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    @JsonIgnore
    public String text() {
        StringBuilder text = new StringBuilder()
                .append(title).append(" nº ").append(reservationId).append('\n')
                .append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n")
                .append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n")
                .append("Entrada: ").append(DATE.format(checkIn))
                .append(" | Saída: ").append(DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        return text.append("Total: ").append(Money.format(total)).toString();
    }
}
