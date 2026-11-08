package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, Room room) {
        return new Receipt("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), room.number(),
                room.type(), r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    /** Texto do comprovante, sem quebra de linha no fim. */
    public String text() {
        StringBuilder out = new StringBuilder();
        out.append(title).append(" nº ").append(reservationId).append('\n');
        out.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        out.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        out.append("Entrada: ").append(checkIn.format(BR_DATE)).append(" | Saída: ").append(checkOut.format(BR_DATE))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        out.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            out.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        out.append("Total: ").append(Money.brl(total));
        return out.toString();
    }
}
