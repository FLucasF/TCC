package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva (o mesmo na confirmação, no check-out e na segunda via). */
public record Receipt(String title, int reservationId, String guestName, String guestCpf,
                      int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                      int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r) {
        return new Receipt("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(),
                r.room().number(), r.room().type(), r.checkIn(), r.checkOut(),
                r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String text() {
        StringBuilder out = new StringBuilder();
        out.append(title).append(" nº ").append(reservationId).append('\n');
        out.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        out.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        out.append("Entrada: ").append(BR_DATE.format(checkIn))
                .append(" | Saída: ").append(BR_DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        out.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            out.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        out.append("Total: ").append(Money.brl(total));
        return out.toString();
    }
}
