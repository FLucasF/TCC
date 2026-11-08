package com.marealta.pousada.dto;

import com.marealta.pousada.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentDto(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    int roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    long nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentDto from(Reservation reservation) {
        return new DocumentDto(
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
