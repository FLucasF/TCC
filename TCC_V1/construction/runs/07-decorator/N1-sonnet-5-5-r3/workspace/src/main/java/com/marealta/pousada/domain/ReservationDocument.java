package com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record ReservationDocument(String title, long reservationId, String guestName, String guestCpf,
                                  int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                                  int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static ReservationDocument of(Reservation reservation, Room room) {
        Quote quote = reservation.quote();
        return new ReservationDocument("Comprovante de reserva", reservation.id(), reservation.guestName(),
                reservation.guestCpf(), room.number(), room.type(), reservation.checkIn(), reservation.checkOut(),
                quote.nights(), quote.subtotal(), quote.discount(), quote.total());
    }

    /** Texto do comprovante, usado no e-mail de confirmação. */
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(DATE.format(checkIn)).append(" | Saída: ").append(DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Money.format(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.format(discount)).append('\n');
        }
        text.append("Total: ").append(Money.format(total));
        return text.toString();
    }
}
