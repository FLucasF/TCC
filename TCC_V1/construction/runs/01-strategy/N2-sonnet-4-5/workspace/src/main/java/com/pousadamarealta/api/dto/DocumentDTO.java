package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentDTO(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    long nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentDTO from(Reservation reservation) {
        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            reservation.getRoomType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
