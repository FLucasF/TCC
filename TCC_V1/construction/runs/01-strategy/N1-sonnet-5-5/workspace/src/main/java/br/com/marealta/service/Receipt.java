package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva paga: o mesmo na confirmação, no check-out e na segunda via. */
public record Receipt(
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

    public static final String TITLE = "Comprovante de reserva";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r) {
        Room room = Room.find(r.roomNumber()).orElseThrow();
        return new Receipt(r.id(), r.guestName(), r.guestCpf(), r.roomNumber(), room.type().name(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(TITLE).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(DATE.format(checkIn)).append(" | Saída: ").append(DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        text.append("Total: ").append(Money.brl(total));
        return text.toString();
    }
}
