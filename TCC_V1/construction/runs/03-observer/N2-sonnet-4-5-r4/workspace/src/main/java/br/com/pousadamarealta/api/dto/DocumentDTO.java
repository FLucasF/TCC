package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public record DocumentDTO(
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
    public static DocumentDTO from(Reservation reservation, RoomType roomType,
                                   BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            roomType.name(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getNights(),
            subtotal,
            discount,
            total
        );
    }
}
