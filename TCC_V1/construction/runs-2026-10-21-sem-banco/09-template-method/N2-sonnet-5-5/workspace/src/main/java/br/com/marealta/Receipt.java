package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, long nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r) {
        return new Receipt("Comprovante de reserva", r.getId(), r.getGuestName(), r.getGuestCpf(),
                r.getRoomNumber(), r.room().type(), r.getCheckIn(), r.getCheckOut(), r.getNights(),
                r.getSubtotal(), r.getDiscount(), r.getTotal());
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
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
