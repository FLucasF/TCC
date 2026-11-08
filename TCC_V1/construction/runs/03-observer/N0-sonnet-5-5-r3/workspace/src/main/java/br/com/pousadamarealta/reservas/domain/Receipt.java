package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva (e segunda via). */
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
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation reservation, Room room) {
        return new Receipt(TITLE, reservation.id(), reservation.guestName(), reservation.guestCpf(),
                room.number(), room.type(), reservation.checkIn(), reservation.checkOut(), reservation.nights(),
                reservation.subtotal(), reservation.discount(), reservation.total());
    }

    public static String formatDate(LocalDate date) {
        return BR_DATE.format(date);
    }

    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(formatDate(checkIn)).append(" | Saída: ").append(formatDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        text.append("Total: ").append(Money.format(total));
        return text.toString();
    }
}
