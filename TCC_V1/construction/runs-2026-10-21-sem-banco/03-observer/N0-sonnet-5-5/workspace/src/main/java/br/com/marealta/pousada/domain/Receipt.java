package br.com.marealta.pousada.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, int reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), r.room().number(), r.room().type(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    /** Human-readable receipt, as sent by e-mail. */
    @JsonIgnore
    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(DATE.format(checkIn)).append(" | Saída: ").append(DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        sb.append("Total: ").append(Money.format(total));
        return sb.toString();
    }
}
