package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.Formatter;
import br.com.pousadamarealta.domain.Reservation;
import java.math.BigDecimal;

public record DocumentDto(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentDto from(Reservation reservation) {
        return new DocumentDto(
            "Comprovante de reserva",
            reservation.id(),
            reservation.guestName(),
            Formatter.formatCpf(reservation.guestCpf()),
            reservation.roomNumber(),
            reservation.roomType().name(),
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut()),
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total()
        );
    }
}
