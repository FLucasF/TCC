package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static Receipt of(Reservation r) {
        Quote q = r.quote();
        return new Receipt("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), r.room().number(),
                r.room().type(), r.checkIn(), r.checkOut(), q.nights(), q.subtotal(), q.discount(), q.total());
    }

    public static String formatDate(LocalDate date) {
        return DATE.format(date);
    }

    public String toText() {
        StringBuilder text = new StringBuilder()
                .append(title).append(" nº ").append(reservationId).append('\n')
                .append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n")
                .append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n")
                .append("Entrada: ").append(formatDate(checkIn)).append(" | Saída: ").append(formatDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        return text.append("Total: ").append(Money.format(total)).toString();
    }
}
