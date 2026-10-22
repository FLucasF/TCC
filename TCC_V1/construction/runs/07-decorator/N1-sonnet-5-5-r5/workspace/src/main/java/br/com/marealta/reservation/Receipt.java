package br.com.marealta.reservation;

import br.com.marealta.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva: serve para o e-mail, o check-out e a segunda via. */
public record Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                      LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation reservation, Room room) {
        return new Receipt(reservation.id(), reservation.guestName(), reservation.guestCpf(), room.number(),
                room.type(), reservation.checkIn(), reservation.checkOut(), reservation.nights(),
                reservation.subtotal(), reservation.discount(), reservation.total());
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
