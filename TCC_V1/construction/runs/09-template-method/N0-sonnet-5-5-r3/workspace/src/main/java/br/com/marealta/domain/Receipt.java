package br.com.marealta.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** The "comprovante de reserva": JSON view plus the text version used in e-mails. */
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
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, Room room) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), room.number(), room.type(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    @JsonIgnore
    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(checkIn.format(DATE)).append(" | Saída: ").append(checkOut.format(DATE))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(total));
        return sb.toString();
    }
}
