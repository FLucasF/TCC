package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, Room room) {
        return new Receipt("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), room.number(),
                room.type(), r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(guestCpf)).append(")\n");
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

    private static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
